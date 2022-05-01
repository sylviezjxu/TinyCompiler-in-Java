package IR.Instruction;

public class ConstantInstruction extends Instruction {

    private final int value;

    public ConstantInstruction(int id, int value) {
        super(id, OP.CONST);
        this.value = value;
    }
}
