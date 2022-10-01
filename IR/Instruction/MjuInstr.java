package IR.Instruction;

public class MjuInstr extends Instruction {

    Instruction arg;
    Integer registerId;

    public MjuInstr(Instruction arg, Integer registerId) {
        super(Op.MJU);
        this.arg = arg;
        this.registerId = registerId;
    }

    public String toString() {
        return String.format("%s (%d) #R%d", super.toString(), arg.getId(), registerId);
    }
}
