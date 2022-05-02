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

    public Instruction getOp1() {
        return op1;
    }

    public void setOp1(Instruction op1) {
        this.op1 = op1;
    }

    public Instruction getOp2() {
        return op2;
    }

    @Override
    public String toString() {
        if (op1 == null && op2 == null) {
            return super.toString() + " null null";
        }
        else if (op2 == null) {
            return String.format("%s (%d) null", super.toString(), op1.getId());
        }
        else if (op1 == null) {
            return String.format("%s null (%d)", super.toString(), op2.getId());
        }
        else {
            return String.format("%s (%d) (%d)", super.toString(), op1.getId(), op2.getId());
        }
    }
}
