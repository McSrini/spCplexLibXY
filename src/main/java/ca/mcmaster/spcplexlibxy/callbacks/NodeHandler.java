/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spcplexlibxy.callbacks;

import static ca.mcmaster.spcplexlibxy.Constants.*;
import static ca.mcmaster.spcplexlibxy.Parameters.*;
import ca.mcmaster.spcplexlibxy.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;

/**
 *
 * @author srini
 */
public class NodeHandler extends IloCplex.NodeCallback{

    protected void main() throws IloException {
        if(getNremainingNodes64()>ZERO){
            //PICK THE node with the best lp relax
            int bestIndex = ZERO;
            double bestRelax = IS_MAXIMIZATION? Double.MIN_VALUE: Double.MAX_VALUE;
            for (int index = ZERO; index <getNremainingNodes64(); index++){
                //NodeAttachment  node = (NodeAttachment)  getNodeData(index);
                //double thisLPRelax = node.getLpRelaxValue();
                if (IS_MAXIMIZATION==(bestRelax<getObjValue(index))){
                    bestRelax = getObjValue(index);
                    bestIndex = index;
                }
            }
            selectNode(bestIndex);
        }
    }
    
}
