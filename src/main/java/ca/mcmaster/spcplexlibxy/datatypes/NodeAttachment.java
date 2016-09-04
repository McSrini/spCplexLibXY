package ca.mcmaster.spcplexlibxy.datatypes;

import static ca.mcmaster.spcplexlibxy.Constants.*;
import java.io.Serializable;
import java.util.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry; 
 

/**
 * 
 * @author srini
 *
 * This object is attached to every tree node, and can be used to reconstruct migrated leaf nodes.
 * 
 * It contains branching variable bounds, and other useful information.
 * 
 */
public class NodeAttachment implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    //every time there is a branching on a variable, we update on of these lists with the
    //new bound corresponding to the branching condition
    //
    //the key is the Variable name
    //
    //Note that this list may also have bounds on non-branching variables 
    //
    private Map< String, Double > upperBounds  = new HashMap< String, Double >();
    private Map< String, Double > lowerBounds = new HashMap< String, Double >();
    
    //meta data contains some non-essential information, which can be used for load balancing etc.
    private NodeAttachmentMetadata nodeMetadata = new NodeAttachmentMetadata();
    
    //constructors    
    public NodeAttachment (){
        nodeMetadata = new NodeAttachmentMetadata();
        nodeMetadata.lpRelaxValue= ZERO;
        nodeMetadata.distanceFromOriginalRoot=ZERO;
        nodeMetadata.distanceFromSubtreeRoot=ZERO;
    }
     
    
    public NodeAttachment (    Map< String, Double > upperBounds, 
            Map< String, Double > lowerBounds,  int distanceFromOriginalRoot, int distanceFromSubtreeRoot, double lpRelaxValue) {
         
        this.upperBounds = new HashMap< String, Double >();
        this.lowerBounds = new HashMap< String, Double >();
        for (Entry <String, Double> entry : upperBounds.entrySet()){
            this.upperBounds.put(entry.getKey(), entry.getValue()  );
        }
        for (Entry <String, Double> entry : lowerBounds.entrySet()){
            this.lowerBounds.put(entry.getKey(), entry.getValue()  );
        }
        
        nodeMetadata = new NodeAttachmentMetadata();
        nodeMetadata.lpRelaxValue= lpRelaxValue;
        nodeMetadata.distanceFromOriginalRoot=distanceFromOriginalRoot;
        nodeMetadata.distanceFromSubtreeRoot=distanceFromSubtreeRoot;
         
    }
    

    public String toString() {
        String result = nodeMetadata.distanceFromOriginalRoot + NEWLINE;
        result += nodeMetadata.distanceFromSubtreeRoot+ NEWLINE+ nodeMetadata.lpRelaxValue +NEWLINE;
        for (Entry entry : upperBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        for (Entry entry : lowerBounds.entrySet()) {
            result += entry.getKey()+BLANKSPACE + entry.getValue()+ NEWLINE;
                    
        }
        return result;
    }
    
    public int getDepthFromOriginalRoot(  ){
        return nodeMetadata.distanceFromOriginalRoot  ;
    }
    
    public void setDepthFromSubtreeRoot(int depth ){
        this.nodeMetadata.distanceFromSubtreeRoot = depth;
    }
    
    public int getDepthFromSubtreeRoot(){
        return nodeMetadata.distanceFromSubtreeRoot  ;
    }
    
    public double getLpRelaxValue() {
        return this.nodeMetadata.lpRelaxValue;
    }
    
    public void setLPRelaxValue(double val) {
        this.nodeMetadata.lpRelaxValue=val;
    }

    public Map< String, Double >   getUpperBounds   () {
        return  upperBounds ;
    }

    public Map< String, Double >   getLowerBounds   () {
        return  lowerBounds ;
    }

        
    
}
