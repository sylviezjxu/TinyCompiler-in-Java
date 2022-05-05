package IR.Instruction;

public class Instruction
{
    public static int idCounter = 1;

    private final int id;
    private final Op opType;

    public enum Op {
        CONST, NEG,
        ADD, SUB, MUL, DIV, CMP,
        ADDA, LOAD, STORE, PHI, END,
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

    public boolean isBinary() {
        return opType == Op.ADD || opType == Op.SUB || opType == Op.MUL || opType == Op.DIV ||
                opType == Op.CMP || opType == Op.STORE || opType == Op.PHI;
    }

    public boolean isUnary() {
        return opType == Op.NEG || opType == Op.LOAD || opType == Op.BRA || opType == Op.BNE ||
                opType == Op.BEQ || opType == Op.BLE || opType == Op.BLT || opType == Op.BGE ||
                opType == Op.BGT || opType == Op.WRITE;
    }

    public String toString() {
        return String.format("%d: %s", id, opType.toString());
    }
}
