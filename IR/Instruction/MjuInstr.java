package IR.Instruction;

public class MjuInstr extends Instruction {

    Instruction arg;
    Integer registerId;

    public MjuInstr(Instruction arg, Integer registerId) {
        super(Op.MJU);
        this.arg = arg;
        this.registerId = registerId;
    }
}
