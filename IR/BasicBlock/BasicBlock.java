package IR.BasicBlock;

import IR.Instruction.BinaryInstr;
import IR.Instruction.Instruction;
import IR.Instruction.UnaryInstr;
import IR.Search.InstrSearchNode;
import IR.Search.OpSearcher;

import java.util.*;

/**                     ------------------- BASIC BLOCK CLASS ---------------------
 *
 *  BLOCK TYPES:    a hashset of all types this block can be considered as. Had it as just one blocktype before, but
 *                  might mess up CFG generation which generates blocks based on current's blocktype.
 *
 *  BLOCK POINTERS: --- fallThruTo, branchTo, fallThruFrom, branchFrom ---
 *                  each basic block contains 4 pointers to 2 possible children and 2 possible parents. Each pointer
 *                  is specified by its relationship to this block as either Branch or Fallthrough.
 *
 *  INSTRUCTIONS:   each basic block contains a linearly linked list of instructions generated within this block
 *
 *  SYMBOL TABLE:   Identifier ID's mapped to its instruction value. The complete symbol table exists only in un-nested
 *                  join/follow blocks. All nested blocks only keep mappings of identifiers that were assigned within
 *                  that block. Recursively search upstream if cannot find.
 *
 *  OP SEARCHER:    the search data structure which keeps track of instructions in dominator order, used for CSE
 *
 *  */
public class BasicBlock
{
    // static variables for graph debugging
    public static int blockIdCounter = 1;
    public static ArrayList<BasicBlock> allBlocks = new ArrayList<>();

    // BasicBlock attributes
    private final int blockId;
    private final HashSet<BlockType> blockTypes;
    private BasicBlock fallThruTo;
    private BasicBlock branchTo;
    private BasicBlock fallThruFrom;
    private BasicBlock branchFrom;

    // BasicBlock Data Structures
    private final LinkedList<Instruction> instructions;
    private final HashMap<Integer, Instruction> symbolTable;
    private final OpSearcher opSearcher;

    public enum BlockType {
        BASIC,
        IF, IF_THEN, IF_ELSE, IF_JOIN,
        WHILE, WHILE_BODY, WHILE_FOLLOW
    }

    public BasicBlock(BlockType blockType) {
        this.blockId = BasicBlock.blockIdCounter++;
        this.blockTypes = new HashSet<>();
        this.blockTypes.add(blockType);

        this.instructions = new LinkedList<>();
        this.symbolTable = new HashMap<>();
        this.opSearcher = new OpSearcher();

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

    /** returns true if currentBlock (join) is nested within the then-block of an enclosing if structure. Need the third
     *  condition bc elseBlock also falls through to joinBlock */
    public boolean nestedInThenBlock() {
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
            int index = instructions.size()-2;
            instructions.add(index, i);
        }
        // remove dummy instruction
        // when removing the dummy BRANCH_TO, update whatever instruction is branching to that deleted instr
        else if (instructions.size() >= 1 && instructions.getFirst().getOpType() == Instruction.Op.BRANCH_TO) {
            instructions.removeFirst();
            //Instruction.idCounter--;        // decrement idCounter, as if BRANCH_TO was never generated
            instructions.add(i);
            if (branchFrom != null) {
                ((UnaryInstr)branchFrom.instructions.getLast()).setOp(i);
            }
        }
        else {
            instructions.add(i);
        }
        if (i.isAddSubDivMul()) {
            insertOp(i);
        }
    }

    /** adds the id:instruction pair if not exists in currentBlock, if exists, overrides the current value */
    public void setIdentifierToInstr(int id, Instruction i) {
        symbolTable.put(id, i);
    }

    /** rely on each individual block to be able to locate the most recent definition of a variable */
    public Instruction getIdentifierInstruction(int id) {
        if (symbolTable.containsKey(id)) {
            return symbolTable.get(id);
        } else {
            // ifThen, ifElse, ifJoin, whileBody, whileFollow
            if (isBlockType(BlockType.IF_ELSE) || isBlockType(BlockType.WHILE_FOLLOW)) {
                return branchFrom.getIdentifierInstruction(id);
            }
            // if IF_JOIN doesn't have the id, that means there's no phi, which means neither then/else modified it
            else {  // IF_THEN, IF_JOIN, WHILE_BODY
                return fallThruFrom.getIdentifierInstruction(id);
            }
        }
    }

    /** return the identifier ID given an instruction ID. used in PropagateNestedWhile() to find the identifier id referred
     *  to by a certain phi */
    public int getIdentifierFromInstruction(int instrId) {
        for (Map.Entry<Integer, Instruction> set : symbolTable.entrySet()) {
            if (set.getValue().getId() == instrId) {
                return set.getKey();
            }
        }
        return -1;
    }

    /** gives currentBlock a complete copy of its parent's symbolTable */
    public void updateSymbolTableFromParent(BasicBlock parent) {
        HashMap<Integer, Instruction> parentSymTab = parent.getSymbolTable();
        // for while blocks, their fallThruFrom contains the complete symtab
        for (Map.Entry<Integer, Instruction> pair : parentSymTab.entrySet()) {
            if (!symbolTable.containsKey(pair.getKey())) {
                symbolTable.put(pair.getKey(), pair.getValue());
            }
        }
        if (parent.isBlockType(BlockType.WHILE) && parent.getFallThruFrom() != null) {
            HashMap<Integer, Instruction> parentFallThru = parent.getFallThruFrom().getSymbolTable();
            for (Map.Entry<Integer, Instruction> pair : parentFallThru.entrySet()) {
                if (!symbolTable.containsKey(pair.getKey())) {
                    symbolTable.put(pair.getKey(), pair.getValue());
                }
            }
        }
    }

    /**  random method that returns the id of the first instruction in the while block that is not phi
     *  (used for while-phi-propagation) */
    public int getFirstNonPhiInstrId() {
        for (Instruction instr : instructions) {
            if (instr.getOpType() != Instruction.Op.PHI) {
                return instr.getId();
            }
        }
        return -1;
    }

    public Instruction getFirstInstr() {
        return instructions.getFirst();
    }


    // ---------------------- OP-SEARCH METHODS ------------------------ //

    /** adds instruction into opSearcher */
    private void insertOp(Instruction i) {
        opSearcher.addInstruction(i);
    }

    /** finds an already-computed common expression that not only has the same operands, but its operands refer to the
     *  same identifiers */
    public BinaryInstr exactMatchCommonSubExpr(Instruction i) {
        return opSearcher.searchExactMatch((BinaryInstr) i);
    }

    /** checks if the given instruction has already been computed (same operands), returns if it has */
    public BinaryInstr returnIfComputed(Instruction i) {
        return opSearcher.returnIfComputed((BinaryInstr)i);
    }

    /** update this block's opSearcher to have the same headNodes as the other block */
    public void inheritOpSearchFrom(BasicBlock dom) {
        opSearcher.inherit(dom.opSearcher);
    }
}
