package IR;

import IR.Instruction.ConstantInstr;
import IR.Instruction.Instruction;
import IR.Instruction.BinaryInstr;
import IR.Instruction.UnaryInstr;

import java.util.ArrayList;
import java.util.Map;

/** This is a dynamic data structure made up of doubly linked Basic Blocks, and is the SSA Intermediate Representation.
 *  */
public class SSAIR
{
    private final ArrayList<Instruction> instrInGeneratedOrder;  // <- for propogating phi's in while CFG
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
        if (!currentBlock.isEmpty() || currentBlock.getBranchFrom() != null) {
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

        // ------------------------- SSA INSTRUCTION GENERATION METHODS --------------------------- //

    /** searches for and returns constant in headBlock, if not found, insert and return */
    public Instruction addConstantIfNotExists(int c) {
        for (Instruction instr : headBlock.getInstructions()) {
            if ( ((ConstantInstr) instr).getValue() == c ) {
                return instr;
            }
        }
        Instruction res = new ConstantInstr(c);
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
        /** CASE I
         *        1) currentBlock = IF_THEN block
         *        2) currentBlock is the join/follow block of an if=/while- structure nested within the THEN-BLOCK of an enclosing
         *           if-structure.
         *
         *           This happens when the inner-if has assignment statements after "fi"/"od". those assignments
         *           are not in a IF_THEN block, but still are encased in an outer then-block and need to generate phi's.
         *
         *           *this isn't a problem for assignments anywhere else because all other cases in an if-structure
         *           are either in then-block or else-block and would generate phi's accordingly.
         *
         *  value = FIRST operand of phi, look for second operand in enclosing if-Block's symbol table
         *  */
        if ( currentBlock.nestedInThenBlock() ) {
            BasicBlock joinBlock = currentBlock.getFallThruTo();
            // here can assume if the joinBlock has this id in symbol Table, it must be a phi function for this identifier
            if (joinBlock.containsPhiAssignment(id)) {
                ((BinaryInstr)joinBlock.getIdentifierInstruction(id)).setOp1(value);
            }
            else {
                BinaryInstr phi = new BinaryInstr(Instruction.Op.PHI, value, joinBlock.getBranchFrom().getIdentifierInstruction(id));
                phi.setOpIdReferences(id, id);
                insertPhiToJoinBlock(joinBlock, id, phi);
            }
        }
        /** CASE II
         *       1) currentBlock = IF_ELSE block
         *       2) currentBlock is the join-block/follow-block of an if-/while- structure nested within the ELSE-BLOCK
         *          of an enclosing if-structure. Assignment statement after "fi"/"od".
         *
         *  value = SECOND operand of phi, look for first operand in the if-block of the enclosing if-structure
         *
         *  *NOTE: starting serach for first operand in currentBlock.getBranchFrom() will reach the enclosing if-block
         *  eventually. If joinBlock has no phi for that identifier, that means the entire nested if-structure does not
         *  assign to it, therefore will not have it in its symbol table.
         *  */
        else if ( currentBlock.getFallThruTo() != null && currentBlock.getFallThruTo().isBlockType(BasicBlock.BlockType.IF_JOIN) ) {
            BasicBlock joinBlock = currentBlock.getFallThruTo();
            // check if this phi already exists. if this id has already been assigned in joinBlock, it has an existing phi.
            if (joinBlock.containsPhiAssignment(id)) {
                ((BinaryInstr)joinBlock.getIdentifierInstruction(id)).setOp2(value);
            }
            else {
                BinaryInstr phi = new BinaryInstr(Instruction.Op.PHI, currentBlock.getBranchFrom().getIdentifierInstruction(id), value);
                phi.setOpIdReferences(id, id);
                insertPhiToJoinBlock(joinBlock, id, phi);
            }
        }
        /** CASE III
         *       1) currentBlock = WHILE_BODY block
         *       2) currentBlock is the join-block/follow-block of an if-/while- structure nested within the WHILE-BODY
         *          of an enclosing while-structure. Assignment statement after "fi"/"od".
         *
         *  value = SECOND operand of phi, look for first operand in the parent of the WHILE-BLOCK of the enclosing
         *  while-structure.
         * */
        else if ( currentBlock.getBranchTo() != null && currentBlock.getBranchTo().isBlockType(BasicBlock.BlockType.WHILE) ) {
            BasicBlock joinBlock = currentBlock.getBranchTo();
            // here id might exist in symbol table and be null (var decl- no instruction so no new fallthru), have to check for phi
            if (joinBlock.containsPhiAssignment(id)) {
                ((BinaryInstr)joinBlock.getIdentifierInstruction(id)).setOp2(value);
            }
            else {
                Instruction oldValue = joinBlock.getIdentifierInstruction(id);
                BinaryInstr phi = new BinaryInstr(Instruction.Op.PHI, oldValue, value);
                phi.setOpIdReferences(id, id);
                insertPhiToJoinBlock(joinBlock, id, phi);
                propagateWhilePhiDownstream(joinBlock, id, oldValue, phi);        // for while join-blocks, replace all uses of the identifier to the new result
            }
        }
    }

    private void insertPhiToJoinBlock(BasicBlock joinBlock, int id, Instruction phi) {
        joinBlock.insertInstruction(phi);           // inserts instr into joinBlock
        joinBlock.setIdentifierToInstr(id, phi);    // adds {id : instr} to joinBlock's symbol table
        instrInGeneratedOrder.add(phi);
    }

    /** when called, currentBlock is always the inner-join block. Takes in an argument parentBlock that is the if-block
     *  of the inner if-structure. */
    public void propagateNestedIf(BasicBlock parentBlock) {
        // if-join block nested in if-then, fallThrough to the outer-join
        if (parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.IF)) {
            BasicBlock outerJoin = currentBlock.getFallThruTo();
            // when propagate() is called, the inner join either be empty or only have phi functions. propagate all phi
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                propagatePhiInIfThen(innerPhi, outerJoin);
            }
        }
        // if-join nested in if-else, fallThrough to outer-join
        else if (parentBlock.getBranchFrom() != null && parentBlock.getBranchFrom().isBlockType(BasicBlock.BlockType.IF)){
            BasicBlock outerJoin = currentBlock.getFallThruTo();
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                propagatePhiInIfElse(innerPhi, parentBlock, outerJoin);
            }
        }
        // if-join block nested in while-block, branches to outer-join
        else if (parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.WHILE)) {
            BasicBlock outerJoin = currentBlock.getBranchTo();
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                propagatePhiInWhile(innerPhi, outerJoin);
            }
        }
    }

    /** before calling, currentBlock is set to parentBlock */
    public void propagateNestedWhile(BasicBlock parentBlock) {
        /** nested in if-then. Check for cases where
         *  1) whileBlock immediate falls through from outer-if-block
         *  2) whileBlock falls through from an intermediate block which falls through from outer-if-block
         *  */
        if (parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.IF)
                || parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.IF_THEN))
        {
            BasicBlock outerJoin = parentBlock.getBranchTo().getFallThruTo();
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                if (innerPhi.getOpType() != Instruction.Op.PHI) {   // stop when reaches first non-phi
                    break;
                }
                propagatePhiInIfThen(innerPhi, outerJoin);
            }
        }
        /** while block nested in if-else
         *  always falls through from an intermediate block which falls through from outer-if */
        else if (parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.IF_ELSE))
        {
            BasicBlock outerJoin = parentBlock.getBranchTo().getFallThruTo();
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                if (innerPhi.getOpType() != Instruction.Op.PHI) {   // stop when reaches first non-phi
                    break;
                }
                int identifierId = parentBlock.getIdentifierFromInstruction(innerPhi.getId());  // get the id that innerPhi represents
                if (outerJoin.containsPhiAssignment(identifierId)) {
                    ((BinaryInstr)outerJoin.getIdentifierInstruction(identifierId)).setOp2(innerPhi);
                }
                else {
                    // while structure nested in else always has an additional empty block from which it falls through
                    BinaryInstr outerPhi = new BinaryInstr(Instruction.Op.PHI,
                            parentBlock.getFallThruFrom().getBranchFrom().getIdentifierInstruction(identifierId), innerPhi);
                    outerPhi.setOpIdReferences( ((BinaryInstr)innerPhi).getOp1IdReference(),
                                                ((BinaryInstr)innerPhi).getOp2IdReference() );
                    insertPhiToJoinBlock(outerJoin, identifierId, outerPhi);
                }
            }
        }
        /** nested in while block. Check for cases where
         *  1) whileBlock immediate falls through from outer-while-block
         *  2) whileBlock falls through from an intermediate block which falls through from outer-while-block
         *  */
        else if (parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.WHILE)
                || parentBlock.getFallThruFrom() != null && parentBlock.getFallThruFrom().isBlockType(BasicBlock.BlockType.WHILE_BODY))
        {
            BasicBlock outerJoin = parentBlock.getBranchTo().getBranchTo();
            for (Instruction innerPhi : currentBlock.getInstructions()) {
                if (innerPhi.getOpType() != Instruction.Op.PHI) {   // stop when reaches first non-phi
                    break;
                }
                propagatePhiInWhile(innerPhi, outerJoin);
            }
        }
    }

    /** generates phi from an assignment statement in if-then block. Can be nested */
    private void propagatePhiInIfThen(Instruction innerPhi, BasicBlock outerJoin) {
        int identifierId = currentBlock.getIdentifierFromInstruction(innerPhi.getId());  // get the id that innerPhi represents
        if (outerJoin.containsPhiAssignment(identifierId)) {
            ((BinaryInstr)outerJoin.getIdentifierInstruction(identifierId)).setOp1(innerPhi);
        }
        else {
            BinaryInstr outerPhi = new BinaryInstr(Instruction.Op.PHI, innerPhi,
                    outerJoin.getBranchFrom().getIdentifierInstruction(identifierId));
            outerPhi.setOpIdReferences( ((BinaryInstr)innerPhi).getOp1IdReference(),
                                        ((BinaryInstr)innerPhi).getOp2IdReference() );
            insertPhiToJoinBlock(outerJoin, identifierId, outerPhi);
        }
    }

    /** generates phi from an assignment statement in if-else block. Can be nested */
    private void propagatePhiInIfElse(Instruction innerPhi, BasicBlock parentBlock, BasicBlock outerJoin) {
        int identifierId = currentBlock.getIdentifierFromInstruction(innerPhi.getId());  // get the id that innerPhi represents
        // check if a phi for this identifier already exists
        if (outerJoin.containsPhiAssignment(identifierId)) {
            ((BinaryInstr)outerJoin.getIdentifierInstruction(identifierId)).setOp2(innerPhi);
        }
        else {
            // if this phi does not exist in the outerJoin, that means the then block of the outer-if did not modify it,
            // can just obtain other phi operand from outer-if-block
            BinaryInstr outerPhi = new BinaryInstr(Instruction.Op.PHI,
                    parentBlock.getBranchFrom().getIdentifierInstruction(identifierId), innerPhi);
            outerPhi.setOpIdReferences( ((BinaryInstr)innerPhi).getOp1IdReference(),
                    ((BinaryInstr)innerPhi).getOp2IdReference() );
            insertPhiToJoinBlock(outerJoin, identifierId, outerPhi);
        }
    }

    /** Generates phi and propagates phi downstream in outer-while-structure */
    private void propagatePhiInWhile(Instruction innerPhi, BasicBlock outerJoin) {
        int identifierId = currentBlock.getIdentifierFromInstruction(innerPhi.getId()); // identifier represented by this phi
        if (outerJoin.containsPhiAssignment(identifierId)) {
            ((BinaryInstr)outerJoin.getIdentifierInstruction(identifierId)).setOp2(innerPhi);
        }
        else {
            Instruction oldValue = outerJoin.getIdentifierInstruction(identifierId);
            BinaryInstr outerPhi = new BinaryInstr(Instruction.Op.PHI, oldValue, innerPhi);
            outerPhi.setOpIdReferences( ((BinaryInstr)innerPhi).getOp1IdReference(),
                                        ((BinaryInstr)innerPhi).getOp2IdReference() );
            insertPhiToJoinBlock(outerJoin, identifierId, outerPhi);
            propagateWhilePhiDownstream(outerJoin, identifierId, oldValue, outerPhi);
        }
    }

    /** takes an identifierId because the instructions that get replaced while propagating not only need to be
     *  referring to the same instruction, it also needs to be referring to the same specific identifier, as the same
     *  instruction reference could be referring to different identifiers. */
    public void propagateWhilePhiDownstream(BasicBlock whileBlock, int identId, Instruction oldValue, Instruction newValue) {
        int start = whileBlock.getFirstNonPhiInstrId(); // first instruction id in whileBlock thats not phi
        int end = newValue.getId();
        for (int i = start; i < end; i++) {
            Instruction curr = instrInGeneratedOrder.get(i);
            if (curr.isBinary()) {
                ((BinaryInstr)curr).replaceOperands(identId, oldValue, newValue);
            }
            else if (curr.isUnary()) {
                ((UnaryInstr)curr).replaceOperand(identId, oldValue, newValue);
            }
        }
    }

    /** if branchTo is empty, insert dummy instruction for branching.
     *  In the case of while=follow, the dummy instr can be deleted later when inserting actual instructions */
    public void setBranchInstr(BasicBlock parent) {
        if (parent.getBranchTo().getInstructions().isEmpty()) {
            Instruction dummy = new Instruction(Instruction.Op.BRANCH_TO);
            parent.getBranchTo().insertInstruction(dummy);
        }
        ((UnaryInstr)parent.getInstructions().getLast()).setOp( parent.getBranchTo().getFirstInstr() );
    }

                // ------------------------- DEBUGGING METHODS --------------------------- //

    private String getIdentifierName(int id, Map<String, Integer> lexerMap) {
        for (Map.Entry<String, Integer> set : lexerMap.entrySet()) {
            if (set.getValue() == id) {
                return set.getKey();
            }
        }
        return "Not Found";
    }

    public String symbolTableToString(Map<Integer, Instruction> symbolTable, Map<String, Integer> lexerMap) {
        ArrayList<String> idStrs = new ArrayList<>();
        for (Map.Entry<Integer, Instruction> set : symbolTable.entrySet()) {
            idStrs.add(String.format("%s = (%d)", getIdentifierName(set.getKey(), lexerMap), set.getValue().getId()));
        }
        return String.join("|", idStrs);
    }

    public void printCFG(Map<String, Integer> lexerMap) {
        boolean showSymbolTable = true;
        System.out.println("\n---------------- CFG ---------------");
        // generate blocks
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
        // printing block fallThruTo/branchTo relationships
        for (BasicBlock block : BasicBlock.allBlocks) {
            if (block.getFallThruTo() != null) {
                System.out.printf("bb%d:s -> bb%d:n [label=\"fallthroughTo\"];\n", block.getBlockId(), block.getFallThruTo().getBlockId());
            }
            if (block.getBranchTo() != null) {
                System.out.printf("bb%d:s -> bb%d:n [label=\"branchTo\"];\n", block.getBlockId(), block.getBranchTo().getBlockId());
            }
        }
        System.out.println();
        // generate symbol tables
        if (showSymbolTable) {
            for (BasicBlock block : BasicBlock.allBlocks) {
                if (!block.getSymbolTable().isEmpty()) {
                    System.out.printf("st%d [shape=record, label=\"<b>ST%d | {%s}\"];\n", block.getBlockId(), block.getBlockId(),
                            symbolTableToString(block.getSymbolTable(), lexerMap));
                }
            }
            System.out.println();
            // link symbol tables w blocks
            for (BasicBlock block : BasicBlock.allBlocks) {
                if (!block.getSymbolTable().isEmpty()) {
                    System.out.printf("bb%d:e -> st%d:w [color=blue];\n", block.getBlockId(), block.getBlockId());
                }
            }
        }
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
