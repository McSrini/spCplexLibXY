package ca.mcmaster.spcplexlibxy.callbacks;
  
import ca.mcmaster.spcplexlibxy.datatypes.SubtreeMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ca.mcmaster.spcplexlibxy.datatypes.*;
import ca.mcmaster.spcplexlibxy.utilities.UtilityLibrary;
 
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import ilog.cplex.IloCplex.NodeId;
import java.io.IOException;
import java.util.Set;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author srini
 * 
 * 1) accumulates branching conditions and any other variable bounds , into the kids
 * 2) discards nodes, or entire subtree,  which are inferior to already known incumbent
 * 3) implements distributed MIP gap by using the bestKnownGlobalOptimum
 *
 */
public class BranchHandler extends IloCplex.BranchCallback{
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
    
    //meta data of the subtree which we are monitoring
    private SubtreeMetaData treeMetaData;
    
    //best known optimum is used to prune nodes
    private double bestKnownOptimum;
    
    private boolean startFarmingFlag = false;
     
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+
                    BranchHandler.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
            logger.debug("BranchHandler Version 1.0");
        } catch (IOException ex) {
            ///
        }
          
    }
  
    public BranchHandler (SubtreeMetaData metaData) {
        this.  treeMetaData= metaData;       
    }
     
    public void refresh( double bestKnownOptimum) {
        this.bestKnownOptimum=bestKnownOptimum;
      
    } 
  
    /**
     * discard inferior nodes, or entire trees
     * Otherwise branch the 2 kids and accumulate variable bound information
     *   
     */
    protected void main() throws IloException {
       
        if ( getNbranches()> ZERO ){  
            
            //tree is branching
            
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
                NodeAttachment subTreeRoot = treeMetaData.getRootNodeAttachment();
                nodeData=new NodeAttachment (  
                        subTreeRoot.getUpperBounds(), 
                        subTreeRoot.getLowerBounds(),  
                        subTreeRoot.getDepthFromOriginalRoot(), 
                        ZERO, getObjValue());  
                
            }else{
                //just set the lp relax value
                nodeData.setLPRelaxValue( getObjValue());
            }
            //IMPORTANT - HERE WE CAN ALSO INCLUDE BASIS INFO, IF AVAILABLE
            setNodeData(nodeData);
            
            //first check if entire tree can be discarded
            if (canTreeBeDiscarded() || (canNodeBeDiscarded(nodeData.getLpRelaxValue())&&isSubtreeRoot())   ){
                
                //no point solving this tree any longer 
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is getting discarded "); 
                treeMetaData.setEntireTreeDiscardable();
                abort();
                logger.debug("tree disarded " +this.treeMetaData.getGUID());
                
            } else  /*check if this node can be discarded*/ if (canNodeBeDiscarded(nodeData.getLpRelaxValue())) {               
                // this node and its kids are useless
                //if (  isLoggingInitialized) logger.info( this.treeMetaData.getGUID()+         " tree is pruning inferior node "+         ((NodeAttachment)getNodeData()).nodeMetadata.nodeID); 
                prune();  
                logger.debug("node discarded from tree "+this.treeMetaData.getGUID());
            } else      if ( farmingCondition() && !isSubtreeRoot() ) { //never farm subtree root
                //farm out node
                this.treeMetaData.addFarmedNode(nodeData);
                //remove this node from IloCplex object
                prune();   
                logger.debug(" node pruned from tree "+this.treeMetaData.getGUID());
            } else {
                
                // we  create  2 kids
                
                //First, append the 
                //branching conditions so we can pass them on to the kids
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                BranchDirection[ ][]  dirs = new  BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);

                //get bound tightenings
               // Map< IloNumVar,Double > upperBoundTightenings = findIntegerBounds(true);
               // Map< IloNumVar,Double > lowerBoundTightenings = findIntegerBounds(false);
                
                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {    
                    //apply the bound changes specific to this child
                    NodeAttachment thisChild  = UtilityLibrary.createChildNode( nodeData,
                            dirs[childNum], bounds[childNum], vars[childNum]   ); 

                    //apply bound tightenings

                    /*
                    for (Entry<IloNumVar, Double> entry : upperBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , true);
                    }
                    for (Entry<IloNumVar, Double> entry : lowerBoundTightenings.entrySet()){
                        UtilityLibrary. mergeBound(thisChild, entry.getKey().getName(), entry.getValue()  , false);
                    }
                    */

                    //   create the  kid,  and attach node data  to the kid
                    NodeId nodeID = makeBranch(childNum,thisChild );
                    
                    //thisChild.nodeMetadata.nodeID=nodeID.toString();
                    //thisChild.nodeMetadata.parentNodeID=nodeData.nodeMetadata.nodeID;
                    //thisChild.nodeMetadata.parentLPRelaxValue = nodeData.nodeMetadata.lpRelaxValue;
                    

                }//end for 2 kids
                                
            } //and if else
            
            this.treeMetaData.numRemainingActiveLeafs= getNremainingNodes64();
            
        }//end getNbranches()> ZERO
    } //end main
    
    
    private boolean canNodeBeDiscarded (double lpRelaxVal) throws IloException {
        boolean result = false;
        
        result = IS_MAXIMIZATION  ? 
                    (lpRelaxVal< getCutoff()) || (lpRelaxVal <= bestKnownOptimum )  : 
                    (lpRelaxVal> getCutoff()) || (lpRelaxVal >= bestKnownOptimum );

       
        return result;
    }
    
    //can this ILOCLPEX object  be discarded ?
    private boolean canTreeBeDiscarded(  ) throws IloException{     
        
        //note that the tree may have found a feasible solution that is better than 
        //the best  optimum known to the partition.
        //This is beacuse the best optimum known to the partition is updated only after the tree is solved for 2 minutes
        //Hence, in our discard check, we do not simply use the bestKnownOptimum
        
        double bestIntegerFeasibleObjectiveValue = ZERO;
        if ( IS_MAXIMIZATION)  {
            bestIntegerFeasibleObjectiveValue = Math.max(bestKnownOptimum,  getIncumbentObjValue());
        }else {
            bestIntegerFeasibleObjectiveValue = Math.min(bestKnownOptimum,  getIncumbentObjValue());
        }
         
        //|bestnode-bestinteger|/(1e-10+|bestinteger|) 
        //(bestinteger - bestobjective) / (1e-10 + |bestobjective|)
        double relativeMIPGap =  getBestObjValue() - bestIntegerFeasibleObjectiveValue ;        
        if ( IS_MAXIMIZATION)  {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestIntegerFeasibleObjectiveValue  ));
        } else {
            relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(getBestObjValue() ));
        }
        

        boolean mipGapHaltCondition =  RELATIVE_MIP_GAP >= Math.abs(relativeMIPGap)  ;
        
        //also halt if we cannot do better than bestKnownGlobalOptimum
        boolean inferiorityHaltConditionMax = IS_MAXIMIZATION  && (bestKnownOptimum>=getBestObjValue());
        boolean inferiorityHaltConditionMin = !IS_MAXIMIZATION && (bestKnownOptimum<=getBestObjValue());
         
  
        return   mipGapHaltCondition ||  inferiorityHaltConditionMin|| inferiorityHaltConditionMax;       
      
    }
    
    private boolean isSubtreeRoot () throws IloException {
        
        boolean isRoot = true;
        
        if (getNodeData()!=null  ) {
            NodeAttachment thisNodeData =(NodeAttachment) getNodeData();
            if (thisNodeData.getDepthFromSubtreeRoot()>ZERO) {
                
                isRoot = false;
                
            }
        }    
        
        return isRoot;
        
    }
    
    private boolean farmingCondition() throws IloException{
        if( !startFarmingFlag && getNremainingNodes64() > MAXIMUM_LEAF_NODES_PER_SUB_TREE){
            startFarmingFlag=true;
        } else if (startFarmingFlag && getNremainingNodes64() <MINIMUM_LEAF_NODES_PER_SUB_TREE){
            startFarmingFlag=false;
        }
        return startFarmingFlag;
    }
    
    private Map< IloNumVar,Double > findIntegerBounds (boolean isUpperBound) throws IloException {
        Map< IloNumVar,Double > results = new  HashMap< IloNumVar,Double >();
        IloNumVar[] modelIntVars = this.treeMetaData.getIntvars();
        
        double[] values  = isUpperBound ? getUBs(modelIntVars ): getLBs(modelIntVars);

        for (int index = ZERO ; index <modelIntVars.length; index ++ ){

            results.put( modelIntVars[index] , values[index]) ;
        }
        return results;
    }
     
       
    
}
