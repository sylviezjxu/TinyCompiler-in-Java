package IR.Instruction;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends Instruction
{
    private Integer functionId;
    private String fname;

    public FunctionCall(int id, String fname) {
        super(Op.CALL);
        this.functionId = id;
        this.fname = fname;
    }

    public String toString() {
        return String.format("%s %s", super.toString(), fname);
    }
}
