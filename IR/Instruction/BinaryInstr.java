package IR.Instruction;

public class BinaryInstr extends Instruction
{
    private Instruction op1;
    private Instruction op2;
    private Integer op1IdReference;
    private Integer op2IdReference;

    public BinaryInstr(Op opType, Instruction op1, Instruction op2) {
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

    public void setOp2(Instruction op2) {
        this.op2 = op2;
    }

    public void setOpIdReferences(Integer op1, Integer op2) {
        op1IdReference = op1;
        op2IdReference = op2;
    }

    public Integer getOp1IdReference() {
        return op1IdReference;
    }

    public Integer getOp2IdReference() {
        return op2IdReference;
    }

    public void replaceOperands(int identId, Instruction oldValue, Instruction newValue) {
        if (op1.getId() == oldValue.getId() && op1IdReference != null && op1IdReference == identId) {
            op1 = newValue;
        }
        if (op2.getId() == oldValue.getId() && op2IdReference != null && op2IdReference == identId) {
            op2 = newValue;
        }
    }

    @Override
    public String toString() {
        if (op1 == null) {
            return String.format("%s null (%d)", super.toString(), op2.getId());
        }
        else if (op2 == null) {
            return String.format("%s (%d) null", super.toString(), op1.getId());
        }
        else {
            return String.format("%s (%d) (%d)", super.toString(), op1.getId(), op2.getId());
        }
    }
}
