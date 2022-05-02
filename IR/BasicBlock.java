package IR;

import IR.Instruction.Instruction;

import java.util.*;

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

    private final LinkedList<Instruction> instructions;
    private final HashMap<Integer, Instruction> identifierMappedToInstruction;
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

    // ---------- ACCESS/CFG-LINKING METHODS ----------- //
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

    // --------- METHODS FOR INSTRUCTION GENERATION ---------- //

    public boolean isNested() {
        return fallThruTo == null && branchTo == null;
    }

    public LinkedList<Instruction> getInstructions() {
        return instructions;
    }

    public HashMap<Integer, Instruction> getIdentifierMappedToInstruction() {
        return identifierMappedToInstruction;
    }

    public void addVarDecl(int id) {
        identifierMappedToInstruction.put(id, null);
    }

    public void insertInstruction(Instruction i) {
        this.instructions.add(i);
    }

    public void setIdentifierToInstr(int id, Instruction i) {
        identifierMappedToInstruction.put(id, i);
    }

    // UNTESTED
    public Instruction getIdentifierInstruction(int id) {
        // ifBlock and whileBlock have complete mappings of all variables, can just directly retrieve
        if (identifierMappedToInstruction.containsKey(id)) {
            return identifierMappedToInstruction.get(id);
        } else {
            // ifThen, ifElse, ifJoin, whileBody, whileFollow
            // search upward until find if/while, maybe recursive call
            if (isBlockType(BlockType.IF_ELSE) || isBlockType(BlockType.WHILE_FOLLOW)) {
                return branchFrom.getIdentifierInstruction(id);
            }
            // if IF_JOIN doesn't have the id, that means there's no phi, which means neither then/else modified it
            else {  // IF_THEN, IF_JOIN, WHILE_BODY
                return fallThruFrom.getIdentifierInstruction(id);
            }
        }
    }

    public void updateSymbolTableFromParent(BasicBlock parent) {
        HashMap<Integer, Instruction> parentSymTab = parent.getIdentifierMappedToInstruction();
        for (Map.Entry<Integer, Instruction> pair : parentSymTab.entrySet()) {
            if (!identifierMappedToInstruction.containsKey(pair.getKey())) {
                identifierMappedToInstruction.put(pair.getKey(), pair.getValue());
            }
        }
    }
}
