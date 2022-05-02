package IR.Instruction;

public class ConstantInstruction extends Instruction {

    private final int value;

    public ConstantInstruction(int value) {
        super(OP.CONST);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String toString() {
        return String.format("%s #%d", super.toString(), value);
    }
}
