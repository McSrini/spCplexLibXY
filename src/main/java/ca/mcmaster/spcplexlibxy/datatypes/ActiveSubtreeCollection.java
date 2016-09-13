/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibxy.datatypes;
  
import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author srini
 * 
 * a collection of IloCplex objects and raw nodes ( which are yet to be converted to IloCplex objects).
 * 
 * One such collection resides on each partition
 * 
 * raw nodes can be added to this collection, or extracted from this collection
 * 
 * object includes a solve method for solving IloCplex objects. Raw nodes are converted into
 * IloCplex objects JIT
 * 
 */
public class ActiveSubtreeCollection  {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
     
    
    //list of subtrees
    private List <ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree> ();
    //list of node attachments not yet converted into active sub-trees
    private List <NodeAttachment> rawNodeList = new ArrayList<NodeAttachment> ();
    
    //best known LocalSolution, i.e best on ths partition
    private Solution bestKnownLocalSolution  = new Solution () ;
    private double bestKnownLocalOptimum =bestKnownLocalSolution.getObjectiveValue();
        
    //for statistics
    public double totalTimeAllocatedForSolving = ZERO;
    public double totalTimeUsedForSolving = ZERO;
     
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
            logger.debug("ActiveSubtreeCollection Version 1.0");
        } catch (IOException ex) {
            ///
        }
          
    }
     
    //Constructor
    public ActiveSubtreeCollection (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            rawNodeList.add( node );
        }
        
    }
    
    public ActiveSubtreeCollection (   NodeAttachment  node) throws  Exception  {
         
        rawNodeList.add( node );         
        
    }
        
    public ActiveSubtreeCollection (   ) throws  Exception  {
        
    }
    
    public void add (  NodeAttachment attachment ) throws  Exception  {
         rawNodeList.add( attachment) ;
    }
    
    public void add (  List<NodeAttachment> attachmentList) throws  Exception  {
        for (NodeAttachment node : attachmentList){
            rawNodeList.add( node );
        }
        
    }
        
    public  Solution getSolution (){
        return    bestKnownLocalSolution ;
    }
  
    
    public void cullTrees (double cutoff ) throws Exception{
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        for (int index = ZERO; index <activeSubtreeList.size(); index ++ ){
            if (     activeSubtreeList.get(index).isDiscardable()|| 
                     activeSubtreeList.get(index).isSolvedToCompletion() ||
                     activeSubtreeList.get(index).isInferior( cutoff)   ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
        
         
        for (int index: positionsToCull){
            ActiveSubtree removedTree= activeSubtreeList.remove(index);
            removedTree.end();
              
        }
        
        
    }
    
    public  void  removeInferiorRawNodes (double cutoff) throws IloException {
         
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        
        for (int index = ZERO; index <this.rawNodeList.size(); index ++ ){
            if ( rawNodeList.get(index).getLpRelaxValue() <= cutoff && IS_MAXIMIZATION  ) positionsToCull.add(index);
            if ( rawNodeList.get(index).getLpRelaxValue() >= cutoff && !IS_MAXIMIZATION  ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
        
        for (int index: positionsToCull){
           rawNodeList.remove(index);
        }
        
    }
    
    
    
     
    public List<String> getActiveSubtreeIDs () throws Exception {
        List<String>  idList = new ArrayList<String> ();
        
        for (int index = ZERO ; index< activeSubtreeList.size(); index ++ ){
             
            ActiveSubtree subtree = activeSubtreeList.get(index);
            idList.add(subtree.getGUID());
        }
        return idList ;
    }

    
    
    
    
     public List <NodeAttachment> pluckRawNodes (long count ) {
         Collections.shuffle(rawNodeList);//this.rawNodeList
         List<NodeAttachment> retval = new ArrayList< NodeAttachment>();
        
        while  (count > ZERO && count <= rawNodeList.size()) {
            NodeAttachment node = rawNodeList.remove(rawNodeList.size()-ONE );
            
            retval.add(node);
            count --;
            
        }
        return retval;

     }
     
     
  
    public  int getRawNodesCount (){
        return  this.rawNodeList .size();
    }
    
    public int getNumberOFTrees(){
        return this.activeSubtreeList.size();
    }
    
    public int getNumberOFLeafsAcrossAllTrees(){
        int count = ZERO ;
        for (ActiveSubtree subtree : this.activeSubtreeList) {
            count += subtree.getNumberOfLeafs();
        }
        return count;
    }
    
    
    
    //Solve an active sub-tree selected by the tree selection strategy
    public Solution solve  ( Instant endTimeOnWorkerMachine,         
            Solution bestKnownGlobalSolution , int iteratioNumber, int partitionNumber) throws Exception{
                
        logger.info(" iteration "+ iteratioNumber + " ,subtree collection " + partitionNumber +
                " ,solve Started at  " + Instant.now() + " will end at "+ endTimeOnWorkerMachine);
        
        printRawnodeLPRelaxVals();
        
        double timeSliceForPartition = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeAllocatedForSolving += timeSliceForPartition;
               
        //update the local  copy of solution on this partition
        bestKnownLocalSolution=bestKnownGlobalSolution;
        this.bestKnownLocalOptimum = bestKnownGlobalSolution.getObjectiveValue();
        
        //make several passes thru the list of trees in this partition, till time runs out or all trees solved
        for (int pass = ZERO;  ! isHaltFilePresent() ;pass++) {
            
            logger.info(" starting pass  "+pass);
                    
            //check if time expired, or no work-items left to solve
            double wallClockTimeLeft = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
            //do not solve unless at least a few seconds left
            if ( wallClockTimeLeft< MINIMUM_SOLUTION_TIME_SLICE_PER_SUBTREE_IN_SECONDS  ) break;
                        
            long count = this.activeSubtreeList.size()   + this.rawNodeList.size();            
            if (count== ZERO) break;
            
            // exit in case of error or unbounded
            if (bestKnownLocalSolution.isError() ||bestKnownLocalSolution.isUnbounded())  break;
            
            // ramp up is single pass
            if (iteratioNumber==ZERO && pass>ZERO ) break;
            
            //pick a tree to solve and solve it for MIN_SOLUTION_TIME_SLICE_IN_SECONDS   
            int treeSelectedForSolution = getIndexOfTreeToSolve();
            
            
            
            if (treeSelectedForSolution>=ZERO) {
                //solve it for time slice
                ActiveSubtree subtree = activeSubtreeList.get(treeSelectedForSolution);
                
                logger.debug(" Selected this tree for solving "+subtree.getGUID() + " tree has this many active leafs " +subtree.getNumberOfLeafs() );                 
                logger.debug(" Subtree collection has this many  trees " + this.getNumberOFTrees()+" and raw nodes"+this.getRawNodesCount() );
                
                int solutionTimeSlice = (iteratioNumber==ZERO)? RAMPUP_SOLN_TIME_SLICE_IN_SECONDS : SOLN_TIME_SLICE_PER_SUBTREE_IN_SECONDS;
                boolean betterSolutionFound = solve(  subtree, (int)Math.min(Math.round(wallClockTimeLeft),solutionTimeSlice ) ,   iteratioNumber);   

                
                if ( subtree.isDiscardable() || subtree.isSolvedToCompletion() || subtree.isInferior( bestKnownLocalOptimum) ) {
                    if (subtree.isDiscardable()) logger.debug(" tree discarded "+subtree.getGUID());
                    if (subtree.isSolvedToCompletion()) logger.debug(" tree is Solved To Completion "+subtree.getGUID());
                    subtree.end();
                    activeSubtreeList.remove(treeSelectedForSolution);
                } 
                
                //cull inferior trees and raw nodes in case better solution has been found
                if (betterSolutionFound){
                    this.removeInferiorRawNodes(bestKnownLocalOptimum );
                    this.removeInferiorTrees(bestKnownLocalOptimum); 
                }
                    
            } else {
                //nothing left to solve
                break;
            }
        }
        
        double timeWasted = Duration.between(Instant.now(), endTimeOnWorkerMachine).toMillis()/THOUSAND;
        this.totalTimeUsedForSolving +=(timeSliceForPartition-timeWasted);
        
        logger.info(" iteration "+ iteratioNumber + " ,subtree collection " + partitionNumber +" ,solve Ended at  " + Instant.now());
        printRawnodeLPRelaxVals();
        return this.bestKnownLocalSolution;
        
    }//end solve
    
    private boolean solve(ActiveSubtree subtree , int  timeSliceInSeconds, int iteratioNumber) throws Exception{
        
        boolean hasBetterSolutionBeenFound = false;
        
        subtree.setSearchStrategy(USER_SELECTED_SEARCH_STRATEGY); 
        
        //switch to depth first for big trees, unless we are in ramp up
        //if (subtree.isTooBig() && iteratioNumber!=ZERO )    {            
            //subtree.setSearchStrategy( DepthFirst);
            // logger.debug(" swithced to depth first ");
        //}
       
        logger.debug("raw node count before solve is " + this.rawNodeList.size());
        logger.debug(" solving "+subtree.getGUID() );
         
        subtree.solve(timeSliceInSeconds , bestKnownLocalOptimum);
        
        Solution subTreeSolution = subtree.getSolution() ;
        if ( ZERO != (new SolutionComparator()).compare(bestKnownLocalSolution, subTreeSolution)){
            //we have found a better solution

            //update our copies
            bestKnownLocalSolution = subTreeSolution;                
            bestKnownLocalOptimum = subTreeSolution.getObjectiveValue();
            logger.info("bestKnownLocalOptimum updated to "+bestKnownLocalOptimum );

            hasBetterSolutionBeenFound= true;            

        }
        
        logger.debug("Tree has this many active leafs after solving " + subtree.getNumberOfLeafs()) ;
        //spill all the farmed nodes into the local raw node list
        this.rawNodeList.addAll(subtree.pluckOutFarmedNodes() );
        logger.debug("raw node count after solve is " + this.rawNodeList.size());
        
        return hasBetterSolutionBeenFound;
    }
  
 
    
    //pick a tree to solve
    private int getIndexOfTreeToSolve () throws  Exception{
        
        //default is to solve nothing
        int result = -ONE;
        double lpRelaxValueOfWorkItem = ZERO;
        
        //enumerate the cases  
                
        if (this.activeSubtreeList.size()==ZERO && this.rawNodeList.size()==ZERO  ){
            //no work left, solve nothing
        } else {
            
            //pick the tree we should solve next 
            if (this.activeSubtreeList.size()>ZERO){            
                result = getTreeIndexWithBestObjective();
                lpRelaxValueOfWorkItem= activeSubtreeList.get(result).getBestObjValue(); 
            }
                    
            //if MAX_ACTIVE_SUBTREES limit not yet hit on this partition , may  pick one of the raw nodes
            int bestRawNodeIndex = -ONE;
            if (MAX_ACTIVE_SUBTREES >this.activeSubtreeList.size()    && this.rawNodeList.size()>ZERO ) {
                bestRawNodeIndex = getRawNodeIndexWithBestObjective();
                if (bestRawNodeIndex >= ZERO && shouldPromoteBestRawNodeToTree (result, bestRawNodeIndex))  {
                                             
                    lpRelaxValueOfWorkItem=  rawNodeList.get(bestRawNodeIndex).getLpRelaxValue();
                            
                    // promote it to tree and pick it
                    activeSubtreeList.add(new ActiveSubtree(this.rawNodeList.remove(bestRawNodeIndex)));
                    result = this.activeSubtreeList.size()-ONE;
                   
                }
            }  
            
             
        }
        if(result >=ZERO)logger.debug(" Solve node with lp relax " +lpRelaxValueOfWorkItem);
        return result;
    }
    
    private boolean shouldPromoteBestRawNodeToTree (int bestTreeIndex, int bestRawNodeIndex) throws IloException, Exception {
        boolean result = false ;
        
        if (MAX_ACTIVE_SUBTREES >this.activeSubtreeList.size()  ) {
            if (bestTreeIndex<ZERO) {
                result=true;
            } else {
                //compare LP relax values
                result = (IS_MAXIMIZATION ==(this.activeSubtreeList.get(bestTreeIndex).getBestObjValue() < this.rawNodeList.get(bestRawNodeIndex).getLpRelaxValue()));
            }
        }
        
        return result;
    }
       
 
    //find the node with the best LP relax value
    private int getRawNodeIndexWithBestObjective()  {
         
        int bestIndex = -ONE;
        double bestVal =      IS_MAXIMIZATION? MINUS_INFINITY: PLUS_INFINITY;
        for ( int index = ZERO; index < getRawNodesCount() ; index ++){
             if (IS_MAXIMIZATION &&  rawNodeList.get(index).getLpRelaxValue() > bestVal) {
                 bestIndex= index;
                 bestVal=rawNodeList.get(index).getLpRelaxValue();
             }
             if (!IS_MAXIMIZATION && rawNodeList.get(index).getLpRelaxValue() < bestVal) {
                 bestIndex= index;
                 bestVal=rawNodeList.get(index).getLpRelaxValue();
             }
        } 
        
        return bestIndex;
    }
   
    
    private int getTreeIndexWithBestObjective() throws Exception{
        
        int bestIndex=-ONE;
        double bestVal =  IS_MAXIMIZATION? MINUS_INFINITY: PLUS_INFINITY;
        for ( int index = ZERO; index < this.getNumberOFTrees() ; index ++){
            ActiveSubtree subtree = activeSubtreeList.get(index);
             
            if (IS_MAXIMIZATION &&   subtree.getBestObjValue() > bestVal) {
                 bestIndex=  index;
                 bestVal= subtree .getBestObjValue();
            }
            if (!IS_MAXIMIZATION &&  subtree.getBestObjValue() < bestVal) {
                bestIndex=  index;
                bestVal= subtree .getBestObjValue();
            }
        }

        return bestIndex;
    }
    
   
        
    private static boolean isHaltFilePresent (){
        File file = new File(HALT_FILE);
        return file.exists();
    }
    
    //remove trees which are inferior to a supplied cutoff
    private  List <String>  removeInferiorTrees (double cutoff) throws IloException {
        List <Integer> positionsToCull = new ArrayList <Integer> ();
        List <String> removedTrees = new ArrayList <String> ();
        
        for (int index = ZERO; index <activeSubtreeList.size(); index ++ ){
            if ( activeSubtreeList.get(index).getBestObjValue() <= cutoff && IS_MAXIMIZATION  ) positionsToCull.add(index);
            if ( activeSubtreeList.get(index).getBestObjValue() >= cutoff && !IS_MAXIMIZATION  ) positionsToCull.add(index);
        }
        
        //get indices in decreasing order
        Collections.reverse(  positionsToCull);
        
        for (int index: positionsToCull){
            ActiveSubtree removedTree= activeSubtreeList.remove(index);
            removedTrees.add(removedTree.getGUID());
            removedTree.end();
        }
        
        return removedTrees;
    }
           
    private List<Double> printRawnodeLPRelaxVals(){
        List<Double> vals = new ArrayList<Double>();
        for(int index= ZERO; index < this.rawNodeList.size(); index++){
            vals.add(rawNodeList.get(index).getLpRelaxValue());
        }
        Collections.sort(vals);
        logger.debug("________________ printing lp relax vals:");
        for(int index= ZERO; index < this.rawNodeList.size(); index++){
            logger.debug(vals.get(index));
        }
        logger.debug("----------------");
        return vals;
    }

}
