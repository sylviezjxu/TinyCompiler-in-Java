package IR.Instruction;

public class Instruction
{
    public static int idCounter = 1;

    private final int id;
    private final Op opType;
    private boolean eliminated;

    public enum Op {
        CONST, NEG,
        ADD, SUB, MUL, DIV, CMP,
        ADDA, LOAD, STORE, PHI, END,
        BRA, BNE, BEQ,
        BLE, BLT, BGE, BGT,
        READ, WRITE, WRITENL,
        BRANCH_TO
    }

    public Instruction(Op opType) {
        this.opType = opType;
        this.id = Instruction.idCounter++;
        this.eliminated = false;
    }

    public void eliminate() {
        this.eliminated = true;
    }

    public void activate() {
        this.eliminated = false;
    }

    public Op getOpType() {
        return opType;
    }

    public int getId() {
        return id;
    }

    public boolean isBinary() {
        return isAddSubDivMul() || opType == Op.CMP || opType == Op.STORE || opType == Op.PHI;
    }

    public boolean isUnary() {
        return opType == Op.NEG || opType == Op.LOAD || opType == Op.BRA || opType == Op.BNE ||
                opType == Op.BEQ || opType == Op.BLE || opType == Op.BLT || opType == Op.BGE ||
                opType == Op.BGT || opType == Op.WRITE;
    }

    public boolean isAddSubDivMul() {
        return opType == Op.ADD || opType == Op.SUB || opType == Op.MUL || opType == Op.DIV;
    }

    public String toString() {
        if (eliminated) {
            return String.format("[eliminated] %d: %s", id, opType.toString());
        }
        else {
            return String.format(" %d: %s", id, opType.toString());
        }
    }
}
