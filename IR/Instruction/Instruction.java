package IR.Instruction;

public class Instruction
{
    public static int idCounter = 1;

    private final int id;
    private final Op opType;
    private Integer eliminatedBy;

    public enum Op {
        CONST, NEG,
        ADD, SUB, MUL, DIV, CMP,
        ADDA, LOAD, STORE, PHI, MJU, END,
        BRA, BNE, BEQ,
        BLE, BLT, BGE, BGT,
        READ, WRITE, WRITENL,
        BRANCH_TO, CALL,
        REG
    }

    public Instruction(Op opType) {
        this.opType = opType;
        this.id = Instruction.idCounter++;
    }

    /** returns true if instruction is eliminated and should not be considered for codegen */
    public boolean isEliminated() {
        return this.eliminatedBy != null;
    }

    /** eliminates this instruction */
    public void setEliminatedBy(Integer i) {
        this.eliminatedBy = i;
    }

    /** returns id of the common subexpression that eliminated this instruction */
    public Integer getEliminatedBy() {
        return eliminatedBy;
    }

    /** re-activates this instruction */
    public void activate() {
        this.eliminatedBy = null;
    }

    public Op getOpType() {
        return opType;
    }

    public int getId() {
        return id;
    }

    public boolean isBinary() {
        return isAddSubDivMul() || opType == Op.CMP || opType == Op.STORE || opType == Op.PHI || opType == Op.MJU;
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
        if (isEliminated()) {
            return String.format("[eliminated by (%d)] %d: %s", eliminatedBy, id, opType.toString());
        }
        else {
            return String.format(" %d: %s", id, opType.toString());
        }
    }
}
