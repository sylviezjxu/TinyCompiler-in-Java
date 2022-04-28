package IR;

import IR.Instruction.Instruction;

import java.util.LinkedList;

/**  */
public class BasicBlock
{
    private BlockType blockType;
    private LinkedList<Instruction> instructions;
    private BasicBlock fallthroughBlock;
    private BasicBlock branchBlock;

    public enum BlockType {
        BASIC,
        IF, IF_THEN, IF_ELSE, IF_JOIN,
        WHILE, WHILE_BODY, WHILE_FOLLOW
    }

    public BasicBlock(BlockType blockType, BasicBlock fallThru, BasicBlock branch) {
        this.blockType = blockType;
    }

   public void insertInstruction(Instruction i) {
        this.instructions.add(i);
   }

    public BasicBlock getFallthroughBlock() {
        return fallthroughBlock;
    }

    public void setFallthroughBlock(BasicBlock fallthroughBlock) {
        this.fallthroughBlock = fallthroughBlock;
    }

    public BasicBlock getBranchBlock() {
        return branchBlock;
    }

    public void setBranchBlock(BasicBlock branchBlock) {
        this.branchBlock = branchBlock;
    }
}
