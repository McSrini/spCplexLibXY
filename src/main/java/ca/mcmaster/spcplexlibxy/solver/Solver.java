package ca.mcmaster.spcplexlibxy.solver;

import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ca.mcmaster.spcplexlibxy.callbacks.BranchHandler;
import ca.mcmaster.spcplexlibxy.datatypes.*;
import ilog.concert.IloException;
import ilog.concert.IloLPMatrix;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.BranchDirection;
import java.util.*;
import java.util.Map.Entry;  

public class Solver   {
    
    //this is the CPLEX object we are attached to  
    private IloCplex cplex   ;
    private SubtreeMetaData treeMetaData;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    //and the node handler
    //private NodeHandler nodeHandler ;
         
    public Solver (IloCplex cplex , SubtreeMetaData metaData ) throws Exception{
            
        this.cplex=cplex;
        this.  treeMetaData=  metaData;
        
        branchHandler = new BranchHandler(      metaData   );
        //nodeHandler = new  NodeHandler (    metaData) ;
        
        this.cplex.use(branchHandler);
        //this.cplex.use(nodeHandler);   
        
        setSolverParams();  
    
    }
  
    
    public void setSearchStrategy(int searchStrategy) throws IloException {
        this.cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,  searchStrategy); 
    }
    
    public void setSolverParams() throws IloException {
        //depth first?
        cplex.setParam(IloCplex.Param.MIP.Strategy.NodeSelect,  USER_SELECTED_SEARCH_STRATEGY); 
        
        //MIP gap
        if ( RELATIVE_MIP_GAP>ZERO) cplex.setParam( IloCplex.Param.MIP.Tolerances.MIPGap, RELATIVE_MIP_GAP);

        //others
    }
    
    public boolean isEntireTreeDiscardable() {
        return this.treeMetaData.isEntireTreeDiscardable();
    }
    
    public IloCplex.Status solve(double timeSliceInSeconds,     double bestKnownGlobalOptimum   ) 
            throws  Exception{
        
        
        //can we supply MIP  start along with bestKnownGlobalOptimum ?         
       
        branchHandler.refresh(bestKnownGlobalOptimum);  
        //nodeHandler.setTimeSlice(timeSliceInSeconds);
       
        cplex.setParam(IloCplex.Param.TimeLimit, timeSliceInSeconds); 
        cplex.solve();
        
        //if (  isLoggingInitialized) logger.info("Result of solving is" + cplex.getStatus());
        return cplex.getStatus();
    }
    
}
