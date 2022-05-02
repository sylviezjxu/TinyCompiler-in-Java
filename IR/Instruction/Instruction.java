package IR.Instruction;

public class Instruction
{
    public static int idCounter = 1;

    private final int id;
    private final OP opType;

    public enum OP {
        CONST,
        NEG, ADD, SUB, MUL, DIV,
        CMP, LOAD, STORE,
        PHI,
        BRA, BNE, BEQ,
        BLE, BLT, BGE, BGT,
        READ, WRITE, WRITENL
    }

    public Instruction(OP opType) {
        this.id = Instruction.idCounter++;
        this.opType = opType;
    }

    public int getId() {
        return id;
    }

    public OP getOpType() {
        return opType;
    }

    public String toString() {
        return String.format("%d: %s", id, opType.toString());
    }
}
