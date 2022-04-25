package IR;

import java.util.HashMap;

/** This is a dynamic data structure made up of Basic Blocks, and is the SSA Intermediate Representation. */
public class SSAIR
{
    HashMap<Integer, Instruction> identifierMappedToInstruction;
    int currentBlock;

    public SSAIR() {

        this.currentBlock = 1;
    }
}
