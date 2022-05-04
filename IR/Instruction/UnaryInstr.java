package IR.Instruction;

public class UnaryInstr extends Instruction
{
    private Instruction op;

    public UnaryInstr(Op opType, Instruction op) {
        super(opType);
        this.op = op;
    }

    public String toString() {
        if (op == null) {
            return String.format("%s null", super.toString());
        } else {
            return String.format("%s (%d)", super.toString(), op.getId());
        }
    }
}
