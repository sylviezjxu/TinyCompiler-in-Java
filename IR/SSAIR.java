package IR;

import IR.Instruction.ConstantInstruction;
import IR.Instruction.Instruction;
import IR.Instruction.OpInstruction;

import java.util.HashMap;

/** This is a dynamic data structure made up of Basic Blocks, and is the SSA Intermediate Representation. */
public class SSAIR
{
    private HashMap<Integer, OpInstruction> identifierMappedToInstruction;
    private final BasicBlock headBlock;
    private BasicBlock currentBlock;

    /** initialize headBlock to empty block used to store constants. */
    public SSAIR() {
        headBlock = new BasicBlock(BasicBlock.BlockType.BASIC, null, null);     // headBlock stores constants
        currentBlock = headBlock;
    }

    public void generateFallThruBlock(BasicBlock.BlockType blockType) {
        // generates new block, pointed to by currentBlock. Set currentBlock to new block.
        currentBlock.setFallthroughBlock( new BasicBlock(blockType, null, null) );
        currentBlock = currentBlock.getFallthroughBlock();
    }

    /** add constant to headBlock */
    public Instruction addConstant(int c) {
        Instruction res = new ConstantInstruction(c);
        headBlock.insertInstruction(res);
        return res;
    }

    /** add variable declaration to symbol table and initialize to null */
    public void addVarDecl(int id) {
        identifierMappedToInstruction.put(id, null);
    }

    /** returns Instruction mapped to given identifier id */
    // probably will need to change later, each basic block has different symbol table
    public Instruction getIdentifierInstruction(int id) {
        return this.identifierMappedToInstruction.getOrDefault(id, null);
    }

    /** inserts Instruction into the current block */
    public void insertInstruction(Instruction i) {
        this.currentBlock.insertInstruction(i);
    }

}
