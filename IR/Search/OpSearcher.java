package IR.Search;

import IR.Instruction.BinaryInstr;
import IR.Instruction.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** The search data structure that keeps separates instructions based on opcodes. Used in eliminating common subexpression */
public class OpSearcher {

    private final HashMap<Instruction.Op, InstrSearchNode > linkedOps;

    public OpSearcher() {
        linkedOps = new HashMap<>();
        linkedOps.put(Instruction.Op.ADD, null);
        linkedOps.put(Instruction.Op.SUB, null);
        linkedOps.put(Instruction.Op.MUL, null);
        linkedOps.put(Instruction.Op.DIV, null);
    }

    /** copies over the other's head nodes of each opType into a new OpSearcher */
    public void inherit(OpSearcher other) {
        linkedOps.putAll(other.linkedOps);
    }

    /** Two computations are Common subexpression if they have same OpType and same operands
     *  returns the instruction that's already been computed */
    public BinaryInstr returnIfComputed(BinaryInstr instr) {
        InstrSearchNode current = linkedOps.get(instr.getOpType());
        while (current != null && current.getInstr() != null) {
            BinaryInstr currInstr = (BinaryInstr)current.getInstr();
            if ( currInstr.sameOperandIds(instr) ) {
                return currInstr;
            }
            current = current.getNext();
        }
        return null;
    }

    /** searches for exact match of two binary instructions. Same operands and same operand references */
    public BinaryInstr searchExactMatch(BinaryInstr instr) {
        InstrSearchNode current = linkedOps.get(instr.getOpType());
        while (current != null && current.getInstr() != null) {
            BinaryInstr currInstr = (BinaryInstr)current.getInstr();
            if ( currInstr.sameOperandIdAndRefs(instr) ) {
                return currInstr;
            }
            current = current.getNext();
        }
        return null;
    }

    /** adds given instruction into the front of its corresponding linked list. Set key to point to new head */
    public void addInstruction(Instruction instr) {
        InstrSearchNode oldHead = linkedOps.get(instr.getOpType());
        InstrSearchNode newHead = new InstrSearchNode(instr, oldHead);
        linkedOps.put(instr.getOpType(), newHead);
    }

}
