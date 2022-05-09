package IR.BasicBlock;

import IR.Instruction.Instruction;

import java.util.HashMap;
import java.util.LinkedList;

/** The search data structure that keeps separates instructions based on opcodes. Used in eliminating common subexpression */
public class OpSearch {

    private HashMap<Instruction.Op, LinkedList<Instruction>> opSearcher;

    public OpSearch() {
        opSearcher = new HashMap<>();
        opSearcher.put(Instruction.Op.ADD, new LinkedList<>());
        opSearcher.put(Instruction.Op.SUB, new LinkedList<>());
        opSearcher.put(Instruction.Op.MUL, new LinkedList<>());
        opSearcher.put(Instruction.Op.DIV, new LinkedList<>());
    }

    // copy constructor
    public OpSearch(OpSearch copy) {

    }
}
