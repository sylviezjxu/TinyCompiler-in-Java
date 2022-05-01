package IR;

import IR.Instruction.ConstantInstruction;
import IR.Instruction.Instruction;

import java.util.ArrayList;

/** This is a dynamic data structure made up of doubly linked Basic Blocks, and is the SSA Intermediate Representation. */
public class SSAIR
{
    private ArrayList<Instruction> instrInGeneratedOrder;
    private final BasicBlock headBlock;
    private BasicBlock currentBlock;

    /** initialize headBlock to empty block used to store constants. */
    public SSAIR() {
        instrInGeneratedOrder = new ArrayList<>();
        headBlock = new BasicBlock(BasicBlock.BlockType.BASIC);     // headBlock stores constants
        currentBlock = headBlock;
    }

    public void generateFallThruBlock(BasicBlock.BlockType blockType) {
        // generates new block, pointed to by currentBlock. Set currentBlock to new block.
        BasicBlock newBlock = new BasicBlock(blockType);
        currentBlock.addDoubleLinkedFallThruTo(newBlock);
        currentBlock = currentBlock.getFallThruTo();      // since it's just single path fallthru, advance currentblock to fallthru
    }

    /** add constant to headBlock */
    public Instruction addConstant(int c) {
        Instruction res = new ConstantInstruction(Instruction.idCounter++, c);
        headBlock.insertInstruction(res);
        return res;
    }

    /** add variable declaration to current block's symbol table and initialize to null */
    public void addVarDecl(int id) {
        currentBlock.addVarDecl(id);
    }

    /** given identifier id, returns Instruction value from current block */
    public Instruction getIdentifierInstruction(int id) {
        return currentBlock.getIdentifierInstruction(id);
    }

    /** inserts Instruction into the current block and into list of instrInGeneratedOrder*/
    public void insertInstruction(Instruction i) {
        currentBlock.insertInstruction(i);
        instrInGeneratedOrder.add(i);
    }

    public void setCurrentBlock(BasicBlock target) {
        this.currentBlock = target;
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

    /** FOR DEBUGGING PURPOSES ONLY */
    public void printCFG() {
        for (BasicBlock block : BasicBlock.allBlocks) {
            System.out.printf("bb%d [shape=record, label=\"<b>BB%d\"];\n", block.getBlockId(), block.getBlockId());
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
