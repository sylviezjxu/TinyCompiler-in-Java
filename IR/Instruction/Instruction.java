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
        if (Instruction.idCounter == 4 || Instruction.idCounter == 8 || Instruction.idCounter == 12) {
            System.out.print("Skipped Instruction: ");
            System.out.println(opType);
        }
        System.out.printf("~~~~~~~~~~~~Instruction.idCounter incremented from %d to %d~~~~~~~~~~~~~\n",
                Instruction.idCounter, Instruction.idCounter+1);

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
