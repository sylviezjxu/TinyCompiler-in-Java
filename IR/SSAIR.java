package IR;

import IR.Instruction.ConstantInstruction;
import IR.Instruction.Instruction;
import IR.Instruction.OpInstruction;

import javax.swing.plaf.basic.BasicIconFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** This is a dynamic data structure made up of doubly linked Basic Blocks, and is the SSA Intermediate Representation.
 *  */
public class SSAIR
{
    private ArrayList<Instruction> instrInGeneratedOrder;  // <- for propogating phi's in while CFG
    private final BasicBlock headBlock;
    private BasicBlock currentBlock;

    /** initialize headBlock to empty block used to store constants. */
    public SSAIR() {
        instrInGeneratedOrder = new ArrayList<>();
        instrInGeneratedOrder.add(null);
        headBlock = new BasicBlock(BasicBlock.BlockType.BASIC);     // headBlock stores constants
        currentBlock = headBlock;
    }

            // ------------------------- CFG GENERATION METHODS --------------------------- //

    public BasicBlock getCurrentBlock() {
        return currentBlock;
    }

    /** sets currentBlock to target block. Used to set up CFG to generate instructions in the right blocks */
    public void setCurrentBlock(BasicBlock target) {
        this.currentBlock = target;
    }

    /** generates new block which directly falls thru from currentBlock. Set currentBlock to new block */
    public void generateFallThruBlock(BasicBlock.BlockType blockType) {
        BasicBlock newBlock = new BasicBlock(blockType);
        currentBlock.addDoubleLinkedFallThruTo(newBlock);
        currentBlock = currentBlock.getFallThruTo();      // since it's just single path fallthru, advance currentblock to fallthru
    }

    /** generate IF-CFG. current block becomes if-block, generate then block and join block,
     *  generated CFG is in the form if-then-fi. else block will be generated later if seen. */
    public BasicBlock enterIf()
    {
        BasicBlock outerJoin, newJoin, newThenBlock;
        // for nested-if inside while: new join branches back to while-join
        if (currentBlock.getFallThruFrom() != null && currentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.WHILE)) {
            outerJoin = currentBlock.getBranchTo();
            newJoin = new BasicBlock(BasicBlock.BlockType.IF_JOIN);
            newJoin.addDoubleLinkedBranchTo(outerJoin);
            newJoin.addDoubleLinkedBranchFrom(currentBlock);
        } else {
            // for all other cases: nested in if-then, nested in if-else, un-nested
            // when entering IF, the current block ALWAYS has a fallsThru relationship w the outer join if it exists
            outerJoin = currentBlock.getFallThruTo();   // save outer join block (null if not nested)
            newJoin = new BasicBlock(BasicBlock.BlockType.IF_JOIN);
            newJoin.addDoubleLinkedFallThruTo(outerJoin);
            newJoin.addDoubleLinkedBranchFrom(currentBlock);
        }
        // then block connects the same way for all cases
        newThenBlock = new BasicBlock(BasicBlock.BlockType.IF_THEN);
        newThenBlock.addDoubleLinkedFallThruTo(newJoin);
        newThenBlock.addDoubleLinkedFallThruFrom(currentBlock);

        currentBlock.addBlockType(BasicBlock.BlockType.IF);
        return currentBlock;
    }

    /** generates else-block. accommodates for nested-ness by setting current branch to new branch's fallthrough
     *  can assume when this is called, currentBlock is always an if-block. */
    public void generateElseBlock() {
        BasicBlock join = this.currentBlock.getBranchTo();   // save join block
        // change then->join to branch
        BasicBlock innerJoin = join.getFallThruFrom();       // save then block or join-block inside then-block
        join.deleteFallThruWithParent(innerJoin);       // delete fallThru between outerJoin <-> innerJoin
        innerJoin.addDoubleLinkedBranchTo(join);
        // else block branches from current if-block, falls through to join block
        BasicBlock newElse = new BasicBlock(BasicBlock.BlockType.IF_ELSE);
        newElse.addDoubleLinkedBranchFrom(currentBlock);
        newElse.addDoubleLinkedFallThruTo(join);
    }

    /** finds join-block after generating then-block/else-block. currentBlock is either thenBlock or elseBlock. */
    public BasicBlock findJoinBlock() {
        return currentBlock.getFallThruTo();
    }

    /** generate WHILE-CFG. condition gets it own block (since it's also the join block), body and follow blocks are
     *  both generated here. */
    public BasicBlock enterWhile() {
        // save outer block if outer is a while block, save join block if nested in if
        BasicBlock saveOuter = currentBlock.getFallThruFrom();
        BasicBlock saveJoin = currentBlock.getFallThruTo();
        boolean nestedInWhile = saveOuter != null && saveOuter.isBlockType(BasicBlock.BlockType.WHILE);
        // kill loop branch if nested in while
        if (nestedInWhile) {
            currentBlock.deleteBranchWithParent(saveOuter);
        }
        // while cmp needs to be in its own block for phi generation
        if (!currentBlock.isEmpty()) {                         // if current is empty, no need to generate new block
            generateFallThruBlock(BasicBlock.BlockType.WHILE);      // currentBlock is now the WHILE-Block
            currentBlock.updateSymbolTableFromParent(currentBlock.getFallThruFrom());   // whileBlock should have complete symbol table
        } else {
            currentBlock.addBlockType(BasicBlock.BlockType.WHILE);  // currentBlock is now of BlockType WHILE
        }
        // generate and link while body
        BasicBlock whileBody = new BasicBlock(BasicBlock.BlockType.WHILE_BODY);
        whileBody.addDoubleLinkedFallThruFrom(currentBlock);
        whileBody.addDoubleLinkedBranchTo(currentBlock);
        // generate and link while follow
        BasicBlock whileFollow = new BasicBlock(BasicBlock.BlockType.WHILE_FOLLOW);
        whileFollow.addDoubleLinkedBranchFrom(currentBlock);
        if (nestedInWhile) {
            whileFollow.addDoubleLinkedBranchTo(saveOuter);
        } else {
            whileFollow.addDoubleLinkedFallThruTo(saveJoin);
        }
        return currentBlock;
    }

        // ------------------------- SSA INSTRUCTION GENERATION METHODS --------------------------- //

    /** searches for and returns constant in headBlock, if not found, insert and return */
    public Instruction addConstantIfNotExists(int c) {
        for (Instruction instr : headBlock.getInstructions()) {
            if ( ((ConstantInstruction) instr).getValue() == c ) {
                return instr;
            }
        }
        Instruction res = new ConstantInstruction(c);
        headBlock.insertInstruction(res);
        instrInGeneratedOrder.add(res);
        return res;
    }

    /** add variable declaration to current block's symbol table and initialize to null
     *  since all variables are declared before statementSequence, all declarations get added
     *  to BB2 (headblock's fallThruTo) */
    public void addVarDecl(int id) {
        currentBlock.addVarDecl(id);
    }

    /** inserts Instruction into the current block and into list of instrInGeneratedOrder */
    public void insertInstrToCurrentBlock(Instruction i) {
        currentBlock.insertInstruction(i);
        instrInGeneratedOrder.add(i);
    }

    /** given identifier id, returns Instruction value from current block, search method implemented in BasicBlock */
    public Instruction getIdentifierInstruction(int id) {
        return currentBlock.getIdentifierInstruction(id);
    }

    /** assigned identifier an instr value, handles phi generation, immediately propagate if nested
     *  all assignments get inserted into currentBlock's symbol table */
    public void assign(int id, Instruction value)
    {
        this.currentBlock.setIdentifierToInstr(id, value);
        // only generate phi for if-then, if-else, while-body - blocks that are branches of CFG
        if (currentBlock.isBlockType(BasicBlock.BlockType.IF_THEN) ) {
            BasicBlock joinBlock = currentBlock.getFallThruTo();
            Instruction phi = new OpInstruction(Instruction.OP.PHI, value, joinBlock.getBranchFrom().getIdentifierInstruction(id));
            joinBlock.insertInstruction(phi);           // inserts instr into joinBlock
            joinBlock.setIdentifierToInstr(id, phi);    // adds {id : instr} to joinBlock's symbol table
            instrInGeneratedOrder.add(phi);
        }
        // order of phi operands differ between then/else blocks
        else if (currentBlock.isBlockType(BasicBlock.BlockType.IF_ELSE)) {
            BasicBlock joinBlock = currentBlock.getFallThruTo();
            // check if this phi already exists. if this id has already been assigned in joinBlock, it has an existing phi.
            if (joinBlock.containsAssignment(id)) {
                ((OpInstruction)joinBlock.getIdentifierInstruction(id)).setOp2(value);
            }
            else {
                Instruction phi = new OpInstruction(Instruction.OP.PHI, currentBlock.getBranchFrom().getIdentifierInstruction(id), value);
                joinBlock.insertInstruction(phi);
                joinBlock.setIdentifierToInstr(id, phi);
                instrInGeneratedOrder.add(phi);
            }
        }
        else if (currentBlock.isBlockType(BasicBlock.BlockType.WHILE_BODY)) {
            BasicBlock joinBlock = currentBlock.getBranchTo();
            Instruction phi = new OpInstruction(Instruction.OP.PHI, currentBlock.getFallThruFrom().getIdentifierInstruction(id), value);
            joinBlock.insertInstruction(phi);
            joinBlock.setIdentifierToInstr(id, phi);
            instrInGeneratedOrder.add(phi);
        }
    }

    public void propagateIfJoin() {

    }

                // ------------------------- DEBUGGING METHODS --------------------------- //

    public void printSymbolTable(Map<String, Integer> lexerMap) {
        System.out.println("              _____________________");
        System.out.println(" Constants:   instr id  | constant");
        System.out.println("              __________|__________");
        for (Instruction i : headBlock.getInstructions()) {
            System.out.printf("                    %s | %d\n",
                                String.format("(%d)", i.getId()), ((ConstantInstruction) i).getValue());
        }
        System.out.println();
        System.out.println("                _____________________");
        System.out.println(" Identifiers:       var   | instr    ");
        System.out.println("                __________|__________");

        HashMap<Integer, Instruction> symTab = headBlock.getFallThruTo().getSymbolTable();
        for (Map.Entry<Integer, Instruction> symTabSet : symTab.entrySet()) {
            String varName = "not found";
            for (Map.Entry<String, Integer> lexerSet : lexerMap.entrySet()) {
                if (Objects.equals(lexerSet.getValue(), symTabSet.getKey())) {
                    varName = lexerSet.getKey();
                }
            }
            String value = symTabSet.getValue() == null ? "null" :  String.format( "(%d)", symTabSet.getValue().getId() );
            System.out.printf("                        %s | %s\n", varName, value);
        }
    }

    public void printCFG() {
        System.out.println("\n---------------- CFG ---------------");
        for (BasicBlock block : BasicBlock.allBlocks) {
            ArrayList<String> instrStrs = new ArrayList<>();
            for (Instruction i : block.getInstructions()) {
                instrStrs.add(i.toString());
            }
            String blockContent = String.join("|", instrStrs);
            System.out.printf("bb%d [shape=record, label=\"<b>BB%d | {%s}\"];\n",
                    block.getBlockId(), block.getBlockId(), blockContent);
        }
        System.out.println();
        for (BasicBlock block : BasicBlock.allBlocks) {
            if (block.getFallThruTo() != null) {
                System.out.printf("bb%d:s -> bb%d:n [label=\"fallthroughTo\"];\n", block.getBlockId(), block.getFallThruTo().getBlockId());
            }
            if (block.getBranchTo() != null) {
                System.out.printf("bb%d:s -> bb%d:n [label=\"branchTo\"];\n", block.getBlockId(), block.getBranchTo().getBlockId());
            }
        }
        System.out.println();
//        for (BasicBlock block : BasicBlock.allBlocks) {
//            if (block.getFallThruFrom() != null) {
//                System.out.printf("bb%d:s -> bb%d:n [label=\"fallthroughFrom\"];\n", block.getBlockId(), block.getFallThruFrom().getBlockId());
//            }
//            if (block.getBranchFrom() != null) {
//                System.out.printf("bb%d:s -> bb%d:n [label=\"branchFrom\"];\n", block.getBlockId(), block.getBranchFrom().getBlockId());
//            }
//        }
    }
}
