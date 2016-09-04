/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibxy.datatypes;

 
import static ca.mcmaster.spcplexlibxy.Constants.*;
import java.io.Serializable;

/**
 *
 * @author srini
 * 
 * details of node attachment which can be 
 * used to make load balancing decisions, without having the entire node attachment available
 * 
 */
public class NodeAttachmentMetadata implements Serializable {
        
    // distance From Original Node    , never changes
    public int distanceFromOriginalRoot =ZERO;   
    //    this is  the depth in the current subtree   , may change to 0 if node is migrated
    public int distanceFromSubtreeRoot=ZERO;  
        
    
   
    public double lpRelaxValue=ZERO;
    
    //other warm start information such as basis info and integer infeasibilty can be included here
    
    public NodeAttachmentMetadata(){
      
    }
    
   
    
}

