package IR;

public class Instruction
{
    OP opType;
    Instruction op1;
    Instruction op2;

    public enum OP {
        NEG, ADD, SUB, MUL, DIV,
        CMP, LOAD, STORE,
        PHI,
        BRA, BNE, BEQ,
        BLE, BLT, BGE, BGT,
        READ, WRITE, WRITENL
    }
}
