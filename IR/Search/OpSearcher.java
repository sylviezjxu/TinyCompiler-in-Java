package IR.Search;

import IR.Instruction.BinaryInstr;
import IR.Instruction.Instruction;

import java.util.HashMap;

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

    /** returns true if given instr has been computed already, can do CSE.
     *  Two computations are Common subexpression if they have same OpType and same operands
     *  returns the instruction that's already been computed */
    public Instruction returnIfComputed(BinaryInstr instr) {
        InstrSearchNode current = linkedOps.get(instr.getOpType());
        while (current != null && current.getInstr() != null) {
            BinaryInstr currInstr = (BinaryInstr)current.getInstr();
            if ( currInstr.getOp1().getId() == instr.getOp1().getId() &&
                    currInstr.getOp2().getId() == instr.getOp2().getId() ) {
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
