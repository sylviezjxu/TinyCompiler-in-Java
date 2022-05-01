package IR;

import IR.Instruction.Instruction;
import IR.Instruction.OpInstruction;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**  */
public class BasicBlock
{
    public static int blockIdCounter = 1;
    public static ArrayList<BasicBlock> allBlocks = new ArrayList<>();

    private final int blockId;
    private HashSet<BlockType> blockTypes = new HashSet<>();
    private BasicBlock fallThruTo;
    private BasicBlock branchTo;
    private BasicBlock fallThruFrom;
    private BasicBlock branchFrom;

    private LinkedList<Instruction> instructions;
    private HashMap<Integer, OpInstruction> identifierMappedToInstruction;
    // every basic block should inherit its immediate dominator's symbol table??

    public enum BlockType {
        BASIC,
        IF, IF_THEN, IF_ELSE, IF_JOIN,
        WHILE, WHILE_BODY, WHILE_FOLLOW
    }

    public BasicBlock(BlockType blockType) {
        this.blockId = BasicBlock.blockIdCounter++;
        this.blockTypes.add(blockType);

        this.instructions = new LinkedList<>();
        this.identifierMappedToInstruction = new HashMap<>();

        allBlocks.add(this);
    }

    // ---------- ACCESS METHODS ----------- //
    public int getBlockId() {
        return blockId;
    }

    public boolean isBlockType(BlockType blockType) {
        System.out.printf("bb%d has blocktypes %s\n", this.getBlockId(), blockTypes.toString());
        return this.blockTypes.contains(blockType);
    }

    public void addBlockType(BlockType blockType) {
        this.blockTypes.add(blockType);
    }

    public BasicBlock getFallThruTo() {
        return fallThruTo;
    }

    public BasicBlock getBranchTo() {
        return branchTo;
    }

    public BasicBlock getFallThruFrom() {
        return fallThruFrom;
    }

    public BasicBlock getBranchFrom() {
        return branchFrom;
    }

    public boolean isEmpty() {
        return this.instructions.isEmpty();
    }

    public void addDoubleLinkedBranchTo(BasicBlock branchTo) {
        if (branchTo != null) {
            this.branchTo = branchTo;
            System.out.printf("---------bb%d's branchTo set to bb%d----------\n", this.getBlockId(), branchTo.getBlockId());
            branchTo.branchFrom = this;
        }
    }

    public void addDoubleLinkedBranchFrom(BasicBlock branchParent) {
        if (branchParent != null) {
            this.branchFrom = branchParent;
            System.out.printf("---------bb%d's branchFrom set to bb%d----------\n", this.getBlockId(), branchParent.getBlockId());
            branchParent.branchTo = this;
        }
    }

    public void addDoubleLinkedFallThruTo(BasicBlock fallThruTo) {
        if (fallThruTo != null) {
            this.fallThruTo = fallThruTo;
            System.out.printf("---------bb%d's fallThruTo set to bb%d----------\n", this.getBlockId(), fallThruTo.getBlockId());
            fallThruTo.fallThruFrom = this;
        }
    }

    public void addDoubleLinkedFallThruFrom(BasicBlock fallThruParent) {
        if (fallThruParent != null) {
            this.fallThruFrom = fallThruParent;
            System.out.printf("---------bb%d's fallThruFrom set to bb%d----------\n", this.getBlockId(), fallThruParent.getBlockId());
            fallThruParent.fallThruTo = this;
        }
    }

    public void deleteFallThruWithParent(BasicBlock fallThruParent) {
        if (fallThruParent != null) {
            this.fallThruFrom = null;
            fallThruParent.fallThruTo = null;
        }
    }

    public void deleteBranchWithParent(BasicBlock branchParent) {
        if (branchParent != null) {
            this.branchTo = null;
            System.out.printf("---------bb%d's branch to bb%d deleted----------\n", this.getBlockId(), branchParent.getBlockId());
            branchParent.branchFrom = null;
        }
    }

    // --------- CHANGE METHODS ---------- //
    public void insertInstruction(Instruction i) {
        this.instructions.add(i);
    }

    public void addVarDecl(int id) {
        this.identifierMappedToInstruction.put(id, null);
    }

    public Instruction getIdentifierInstruction(int id) {
        return this.identifierMappedToInstruction.getOrDefault(id, null);
    }
}
