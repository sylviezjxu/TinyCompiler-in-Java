package IR.Instruction;

public class UnaryInstr extends Instruction
{
    private Instruction op;
    private Integer opIdReference;

    public UnaryInstr(Op opType, Instruction op) {
        super(opType);
        this.op = op;
    }

    public Integer getOpIdReference() {
        return opIdReference;
    }

    public void setOpIdReference(Integer opIdReference) {
        this.opIdReference = opIdReference;
    }

    public void setOp(Instruction op) {
        this.op = op;
    }

    public void replaceOperand(int identId, Instruction oldValue, Instruction newValue) {
        // can take out op != null later?? depending on when in Parser I do the branch instr replacement
        if (op!= null && op.getId() == oldValue.getId() && opIdReference != null && opIdReference == identId) {
            op = newValue;
        }
    }

    public String toString() {
        if (op == null) {
            return String.format("%s null", super.toString());
        } else {
            return String.format("%s (%d)", super.toString(), op.getId());
        }
    }
}
