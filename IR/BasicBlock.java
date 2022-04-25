package IR;

import java.util.LinkedList;

public class BasicBlock
{
    int blockId;
    Integer parentBlockId;
    boolean nested;
    LinkedList<Instruction> instructions;
}
