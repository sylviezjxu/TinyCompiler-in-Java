package IR.Instruction;

public class OpInstruction extends Instruction
{
    private Instruction op1;
    private Instruction op2;

    public OpInstruction(Instruction.OP opType, Instruction op1, Instruction op2) {
        super(opType);
        this.op1 = op1;
        this.op2 = op2;
    }
}
