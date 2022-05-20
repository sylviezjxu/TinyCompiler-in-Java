package IR.Search;

import IR.Instruction.Instruction;

public class InstrSearchNode {
    private Instruction instr;
    private InstrSearchNode next;

    public InstrSearchNode(Instruction instr, InstrSearchNode next) {
        this.instr = instr;
        this.next = next;
    }

    public Instruction getInstr() {
        return instr;
    }

    public InstrSearchNode getNext() {
        return next;
    }
}
