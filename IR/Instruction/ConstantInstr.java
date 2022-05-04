package IR.Instruction;

public class ConstantInstr extends Instruction {

    private final int value;

    public ConstantInstr(int value) {
        super(Op.CONST);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
        return String.format("%s #%d", super.toString(), value);
    }
}
