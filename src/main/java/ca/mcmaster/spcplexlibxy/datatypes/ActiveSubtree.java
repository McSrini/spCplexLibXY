package ca.mcmaster.spcplexlibxy.datatypes;
 
import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ca.mcmaster.spcplexlibxy.solver.Solver;
import ca.mcmaster.spcplexlibxy.utilities.UtilityLibrary;
import java.io.IOException; 

import java.util.List;
import java.util.UUID;
 
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.cplex.IloCplex; 
import java.util.ArrayList;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 * 
 * @author Srini
 * 
 * This class is a wrapper around an ILO-CPLEX subtree being solved
 * 
 * note that this object is NOT SERIALIZABLE
 *
 */

public class ActiveSubtree   {
    
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
  
    //the CPLEX object representing this partially solved tree 
    private  IloCplex cplex ;
    
    //meta data about the IloCplex object
    private SubtreeMetaData treeMetaData  ;
        
    //a solver object that is used to solve this tree few seconds at a time 
    private Solver solver ;    
    
    //a flag to indicate if end() has been called on this sub tree
    //Use this method to deallocate memory once this subtree is no longer needed
    private boolean ended = false;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+PARTITION_ID+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (IOException ex) {
            ///
        }
          
    }

    //Constructor
    public ActiveSubtree (  NodeAttachment attachment) throws  Exception  {
        
        //initialize the CPLEX object
        cplex= new IloCplex();   
        cplex.importModel(SAV_FILENAME);
        UtilityLibrary.merge(cplex, attachment); 
        
        IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();        
        treeMetaData = new SubtreeMetaData(   attachment, lp.getNumVars());
        
        //get ourselves a solver
        solver = new Solver( cplex   , treeMetaData);
        
        
            
    }
    
    public long getNumberOfLeafs () {
        return this.treeMetaData.numRemainingActiveLeafs;
    }
    
    public void setSearchStrategy(int searchStrategy) throws IloException {
        this.solver.setSearchStrategy(searchStrategy);
    }
    
    public void end(){
        if (!ended) cplex.end();
        ended=true;
    }
    
    public boolean hasEnded(){
        return         ended ;
    }
    
    public String getGUID(){
        return this.treeMetaData.getGUID();
    }
    
    /**
     * 
     * Solve this subtree for some time
     * Subtree meta data will be updated by the solver.
     */
    public IloCplex.Status solve ( double timeSliceInSeconds,         double bestKnownOptimum )
            throws  Exception {
         
        //solve for some time
        IloCplex.Status  status = solver.solve( timeSliceInSeconds, bestKnownOptimum );
        
        return status;
        
    }
    
    public List<NodeAttachment> pluckOutFarmedNodes(long count){
        
        return this.treeMetaData.pluckFarmedNodes(count);
    }
        
    public List<NodeAttachment> pluckOutFarmedNodes(  ){
        
        return this.treeMetaData.pluckFarmedNodes( );
    }
    
   
        
    public long getFarmedNodesCount () {
        return    this.treeMetaData.getFarmedNodesCount();
    }
 
    public boolean isSolvedToCompletion() throws Exception {
        return   this.isOptimal()||this.isInError()   ||this.isUnFeasible()||this.isUnbounded();
        
    }
    
    public boolean isInferior (double cutoff) throws IloException {
        return (getBestObjValue() <= cutoff && IS_MAXIMIZATION  ) || (getBestObjValue() >= cutoff && !IS_MAXIMIZATION  );
    }
    
    public boolean isDiscardable() {
        return this.treeMetaData.isEntireTreeDiscardable();
    }
    
    public String toString(){
        String details =this.treeMetaData.getGUID() +NEWLINE;
        details += this.treeMetaData.getRootNodeAttachment().toString();
        return details;
        
    }
    
    public double getBestObjValue () throws IloException {
        return this.cplex.getBestObjValue();
    }
    
    public Solution getSolution () throws IloException {
        Solution soln = new Solution () ;
        
        soln.setError(isInError());
        soln.setOptimal(isOptimal());
        soln.setFeasible(isFeasible() );
        soln.setUnbounded(isUnbounded());
        soln.setUnFeasible(isUnFeasible());
        
        soln.setOptimumValue(getObjectiveValue());
        
        if (isOptimalOrFeasible()) UtilityLibrary.addVariablevaluesToSolution(cplex, soln);
        
        return soln;
    }
    
        
    public boolean isFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Feasible) ;
    }
    
    public boolean isUnFeasible () throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Infeasible) ;
    }
    
    public boolean isOptimal() throws IloException {
        
        return cplex.getStatus().equals(IloCplex.Status.Optimal) ;
    }
    public boolean isOptimalOrFeasible() throws IloException {
        return isOptimal()|| isFeasible();
    }
    public boolean isUnbounded() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Unbounded) ;
    }
    
    public boolean isInError() throws IloException {
        return cplex.getStatus().equals(IloCplex.Status.Error) ;
    }
  
    public double getObjectiveValue() throws IloException {
        double inferiorObjective = IS_MAXIMIZATION?  MINUS_INFINITY:PLUS_INFINITY;
        return isFeasible() || isOptimal() ? cplex.getObjValue():inferiorObjective;
    }
        
    public String getStatusString () throws Exception{
        String status = "Unknown";
        if (isUnFeasible())   status =      "Infeasible";
        if (isFeasible()) status = "Feasible";
        if (isOptimal()) status = "optimal.";            
        if (isInError()) status = "error.";       
        if (isUnbounded()) status = "unbounded.";  
        if (this.isDiscardable()) status += " and also discardable.";  
        return status;
    }    
    
}
