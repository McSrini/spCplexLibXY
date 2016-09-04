package ca.mcmaster.spcplexlibxy.datatypes;
 
 
import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ilog.concert.IloNumVar; 

import java.util.*;
import java.util.Map.Entry; 

/**
 * 
 * @author srini
 * 
 * This object holds all the meta data associated with an ActiveSubtree.
 * Since we cannot traverse the ILOCPLEX object at will, we store relevant information here.
 *
 * NOTE - this class need not be serializable
 * 
 */
public class SubtreeMetaData {
     
    //GUID used to identify the ActiveSubtree
    private final String guid ; 
    
    //keep note of the root Node Attachment used to create this subtree
    private final NodeAttachment rootNodeAttachment ;
        
    //here are the nodes which should be solved somewhere else 
    private List< NodeAttachment> farmedNodesList = new ArrayList< NodeAttachment>();
    
    
    //sometimes we find that the entire subtree can be discarded, because it cannot beat the incumbent 
    private boolean canDiscardEntireSubTree  = false;  
    
    //keep note of all the INT variables in the model
    // these are used  to find bound tightenings when spawning kids.
    private final IloNumVar[] intVars ; 
     
    public long numRemainingActiveLeafs =ONE ;
    
    public SubtreeMetaData( NodeAttachment attachment, IloNumVar[] intVars){
        guid = UUID.randomUUID().toString();
        rootNodeAttachment=attachment;
        this.intVars= intVars;
    }
    
    
    
    public String getGUID(){
        return this.guid;
    }
    
 
    
    public IloNumVar[]   getIntvars (){
        return intVars;
    }
    
    public NodeAttachment getRootNodeAttachment(){
        return rootNodeAttachment;
    }
    
    public void addFarmedNode(NodeAttachment node){
        farmedNodesList.add(node);
    }
    
    public long getFarmedNodesCount () {
        return   this.farmedNodesList.size();
    }
    
  
     
    public void setEntireTreeDiscardable() {
        this.canDiscardEntireSubTree= true;
    }
    
    public boolean isEntireTreeDiscardable() {
        return this.canDiscardEntireSubTree ;
    }
    
        
    public List<NodeAttachment> pluckFarmedNodes  () {
        
         
        return pluckFarmedNodes(getFarmedNodesCount());
    }
        
    public List<NodeAttachment> pluckFarmedNodes  (long count) {
        
        //randomize
        Collections.shuffle(farmedNodesList);
          
        List<NodeAttachment> retval = new ArrayList< NodeAttachment>();
        
        while  (count > ZERO && farmedNodesList.size() >ZERO) {
            NodeAttachment node = farmedNodesList.remove(farmedNodesList.size()-ONE );
            
            retval.add(node);
            count --;
            
        }
        return retval;
    }
     
}

