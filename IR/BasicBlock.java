package IR;

import IR.Instruction.Instruction;

import java.util.*;

/**                     ------------------- BASIC BLOCK CLASS ---------------------
 *
 *  BLOCKTYPES:     a hashset of all types this block can be considered as. Had it as just one blocktype before, but
 *                  might mess up CFG generation which generates blocks based on current's blocktype.
 *
 *  BLOCK POINTERS: each basic block contains 4 pointers to 2 possible children and 2 possible parents. Each pointer
 *                  is specified by its relationship to this block as either Branch or Fallthrough.
 *
 *  INSTRUCTIONS:   each basic block contains a linearly linked list of instructions generated within this block
 *
 *  SYMBOL TABLE:   Identifer ID's mapped to its instruction value. The complete symbol table exists only in un-nested
 *                  join/follow blocks. All nested blocks only keep mappings of identifiers that were assigned within
 *                  that block. Recursively search upstream if cannot find.
 *
 *  */
public class BasicBlock
{
    // public variables for debugging purposes
    public static int blockIdCounter = 1;
    public static ArrayList<BasicBlock> allBlocks = new ArrayList<>();

    private final int blockId;
    private HashSet<BlockType> blockTypes = new HashSet<>();
    private BasicBlock fallThruTo;
    private BasicBlock branchTo;
    private BasicBlock fallThruFrom;
    private BasicBlock branchFrom;

    private final LinkedList<Instruction> instructions;
    private final HashMap<Integer, Instruction> symbolTable;


    public enum BlockType {
        BASIC,
        IF, IF_THEN, IF_ELSE, IF_JOIN,
        WHILE, WHILE_BODY, WHILE_FOLLOW
    }

    public BasicBlock(BlockType blockType) {
        this.blockId = BasicBlock.blockIdCounter++;
        this.blockTypes.add(blockType);

        this.instructions = new LinkedList<>();
        this.symbolTable = new HashMap<>();

        allBlocks.add(this);
    }

    // ---------- ACCESS/CFG-LINKING METHODS ----------- //
    public int getBlockId() {
        return blockId;
    }

    public boolean isBlockType(BlockType blockType) {
        //System.out.printf("bb%d has blocktypes %s\n", this.getBlockId(), blockTypes.toString());
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
            //System.out.printf("---------bb%d's branchTo set to bb%d----------\n", this.getBlockId(), branchTo.getBlockId());
            branchTo.branchFrom = this;
        }
    }

    public void addDoubleLinkedBranchFrom(BasicBlock branchParent) {
        if (branchParent != null) {
            this.branchFrom = branchParent;
            //System.out.printf("---------bb%d's branchFrom set to bb%d----------\n", this.getBlockId(), branchParent.getBlockId());
            branchParent.branchTo = this;
        }
    }

    public void addDoubleLinkedFallThruTo(BasicBlock fallThruTo) {
        if (fallThruTo != null) {
            this.fallThruTo = fallThruTo;
            //System.out.printf("---------bb%d's fallThruTo set to bb%d----------\n", this.getBlockId(), fallThruTo.getBlockId());
            fallThruTo.fallThruFrom = this;
        }
    }

    public void addDoubleLinkedFallThruFrom(BasicBlock fallThruParent) {
        if (fallThruParent != null) {
            this.fallThruFrom = fallThruParent;
            //System.out.printf("---------bb%d's fallThruFrom set to bb%d----------\n", this.getBlockId(), fallThruParent.getBlockId());
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
            //System.out.printf("---------bb%d's branch to bb%d deleted----------\n", this.getBlockId(), branchParent.getBlockId());
            branchParent.branchFrom = null;
        }
    }

    // --------- METHODS FOR INSTRUCTION GENERATION ---------- //

    public boolean isNested() {
        return !(fallThruTo == null && branchTo == null);
    }

    public boolean nestedInThenBlock() {
        // returns true if currentBlock (join) is nested within the then-block of an enclosing if structure
        return fallThruTo != null && fallThruTo.isBlockType(BasicBlock.BlockType.IF_JOIN) &&
                fallThruTo.branchFrom.isBlockType(BlockType.IF);
    }

    public LinkedList<Instruction> getInstructions() {
        return instructions;
    }

    public HashMap<Integer, Instruction> getSymbolTable() {
        return symbolTable;
    }

    public boolean containsPhiAssignment(int id) {
        return symbolTable.containsKey(id) && symbolTable.get(id) != null &&
                symbolTable.get(id).getOpType() == Instruction.Op.PHI;
    }

    public void addVarDecl(int id) {
        symbolTable.put(id, null);
    }

    public void insertInstruction(Instruction i) {
        if (isBlockType(BlockType.WHILE) && i.getOpType() == Instruction.Op.PHI) {
            this.instructions.addFirst(i);
        } else {
            this.instructions.add(i);
        }
    }

    /** */
    public void setIdentifierToInstr(int id, Instruction i) {
        symbolTable.put(id, i);
    }

    /** rely on each individual block to be able to locate the most recent definition of a variable */
    // UNTESTED
    public Instruction getIdentifierInstruction(int id) {
        if (symbolTable.containsKey(id)) {
            return symbolTable.get(id);
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

    public int getIdentifierFromInstruction(int instrId) {
        for (Map.Entry<Integer, Instruction> set : symbolTable.entrySet()) {
            if (set.getValue().getId() == instrId) {
                return set.getKey();
            }
        }
        return -1;
    }

    public void updateSymbolTableFromParent(BasicBlock parent) {
        HashMap<Integer, Instruction> parentSymTab = parent.getSymbolTable();
        for (Map.Entry<Integer, Instruction> pair : parentSymTab.entrySet()) {
            if (!symbolTable.containsKey(pair.getKey())) {
                symbolTable.put(pair.getKey(), pair.getValue());
            }
        }
    }

    /** a really random method that returns the id of the first instruction in the while block that is not phi
     *  (used for while-phi-propagation) */
    public int getFirstNonPhiInstrId() {
        for (Instruction instr : instructions) {
            if (instr.getOpType() != Instruction.Op.PHI) {
                return instr.getId();
            }
        }
        return -1;
    }
}
