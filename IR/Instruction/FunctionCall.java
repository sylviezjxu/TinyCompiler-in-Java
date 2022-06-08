package IR.Instruction;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends Instruction
{
    private Integer functionId;
    private List<Instruction> args;

    public FunctionCall() {
        super(Op.CALL);
        args = new ArrayList<>();
    }
}
