package IR.Instruction;

/** instruction used to represent a value stored in a particular register. Used only for argument saved registers.
 *  Used in function SSAIR to represent arguments as instruction values */
public class RegisterInstr extends Instruction
{
    private static int argRgs = 0;
    private final int regId;

    public RegisterInstr() {
        super(Op.REG);
        this.regId = ++argRgs;
    }

    public int getRegId() {
        return regId;
    }

    public String toString() {
        return String.format("%s: argR%d", super.toString(), regId);
    }
}
