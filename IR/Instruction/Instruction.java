package IR.Instruction;

public class Instruction
{
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
        this.opType = opType;
    }

}
