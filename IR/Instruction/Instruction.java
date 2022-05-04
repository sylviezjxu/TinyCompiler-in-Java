package IR.Instruction;

public class Instruction
{
    public static int idCounter = 1;

    private final int id;
    private final Op opType;

    public enum Op {
        CONST, NEG, LOAD,
        ADD, SUB, MUL, DIV,
        CMP, STORE, PHI,
        BRA, BNE, BEQ,
        BLE, BLT, BGE, BGT,
        READ, WRITE, WRITENL
    }

    public Instruction(Op opType) {
        this.opType = opType;
        this.id = Instruction.idCounter++;
    }

    public Op getOpType() {
        return opType;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return String.format("%d: %s", id, opType.toString());
    }
}
