/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibxy;

import static ca.mcmaster.spcplexlibxy.Constants.*;
import java.io.Serializable;

/**
 *
 * @author srini
 */
public class Parameters implements Serializable{
        
    //should move to properties file
    
    //this is the file we are solving
    public static final String SAV_FILENAME="F:\\temporary files here\\msc98-ip.mps";   
    public  static final  boolean IS_MAXIMIZATION = false;
    
    
    public static final String HALT_FILE = "F:\\temporary files here\\haltfile.txt";
    
    public static final int NUM_PARTITIONS =   ONE + THREE*TEN;
    
    //Do not allow any subtree to grow   bigger than this     
    public static int  MAXIMUM_LEAF_NODES_PER_SUB_TREE  =  FIVE* TEN*THOUSAND; 
    public static int  MINIMUM_LEAF_NODES_PER_SUB_TREE  =  MAXIMUM_LEAF_NODES_PER_SUB_TREE/TWO; 
    public final static int  MAXIMUM_LEAF_NODES_PER_SUB_TREE_RAMPUP  =   NUM_PARTITIONS*FOUR; 
    public final static int  MINIMUM_LEAF_NODES_PER_SUB_TREE_RAMPUP  =   ZERO;
    public final static int  MAXIMUM_LEAF_NODES_PER_SUB_TREE_REGULAR  =  TEN*TWO; 
    public final static int  MINIMUM_LEAF_NODES_PER_SUB_TREE_REGULAR  =  ZERO;
                                                                    
    
    // solve each sub tree for this many minutes
    public  static final  int SOLN_TIME_SLICE_PER_SUBTREE_IN_SECONDS  =     TWO*SIX*TEN; 
    //if clock time left is less than a few seconds, do not try to solve another tree on this partition in this iteration
    public  static final  int MINIMUM_SOLUTION_TIME_SLICE_PER_SUBTREE_IN_SECONDS  =     SOLN_TIME_SLICE_PER_SUBTREE_IN_SECONDS/TEN; 
    // ramp up is one big slice, single pass
     public  static final  int RAMPUP_SOLN_TIME_SLICE_IN_SECONDS  =     FIVE*SIX*TEN; 
    
    //limit the number of Iloclex objects in any partition
    //Do not create new ones if this many already active
    public  static final  int MAX_ACTIVE_SUBTREES  =     FOUR; 
        
    //search strategy
    public static final int DepthFirst= ZERO;
    public static final int BreadthFirst= ONE;
    public static final int BestEstimeateFirst= TWO;
    //user can set default strategy
    public static int  USER_SELECTED_SEARCH_STRATEGY = BreadthFirst  ; 
    
    //The partition on which this library (i.e. the ActiveSubtree and supporting objects) live
    public static int  PARTITION_ID = ONE;
    //This is used for logging
    public static String LOG_FOLDER="F:\\temporary files here\\";
    
    
    public static double  RELATIVE_MIP_GAP = ZERO;
    
}
