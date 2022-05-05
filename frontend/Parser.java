package frontend;

import IR.BasicBlock;
import IR.Instruction.Instruction;
import IR.Instruction.BinaryInstr;
import IR.Instruction.UnaryInstr;
import IR.SSAIR;
import errors.TinySyntaxError;

import java.io.IOException;

/** A recursive descent parser based on EBNF for tiny. SSA IR is generated while parsing. */
public class Parser {

    private final Lexer lexer;
    private final SSAIR IR;

    // helper variables for mapping identifier to instruction operands
    private boolean termIsVarRef = false;
    private Integer termVarRefId;
    private boolean exprIsVarRef = false;
    private Integer exprVarRefId;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.IR = new SSAIR();
        computation();
    }

    /** returns the Instruction that is mapped to the variable */
    public Instruction variableReference() {
        System.out.println("variable reference: " + this.lexer.debugToken(peek()));
        Token var = next();
        Instruction value = IR.getIdentifierInstruction(var.getIdValue());
        if (value == null) {
            warning( String.format("variable %s is referenced but never initialized.",
                     lexer.getIdentifierName(var.getIdValue())) );
        }
        return value;
    }

    /** returns the Instruction that represents the value of the literal */
    public Instruction number() {
        // DONE
        System.out.println("number" + this.lexer.debugToken(peek()));
        Token var = next();
        return IR.addConstantIfNotExists(var.getIdValue());
    }

    /** factor() returns the Instruction that represents the value of the var/constant/expression/function call */
    public Instruction factor() {
        // DONE
        System.out.println("factor");
        Instruction res;
        Token peek = peek();
        if (peek.isUserDefinedIdentifier()) {
            res = variableReference();
        }
        else if (peek.isLiteral()) {
            res = number();
        }
        else if (checkIfTokenIs(peek, "(")) {
            next();     // consumes "("
            res = expression();
            next();     // consumes ")"
        }
        else if (checkIfTokenIs(peek, "call")) {
            res = nonVoidFunctionCall();        // returns instr. representing result of the function call
        } else {
            res = null;
        }
        return res;
    }

    /** returns the Instruction that represents the value of the term */
    public Instruction term() {
        // DONE
        System.out.println("term");
        Instruction op1, op2, res;
        // check if op is a variable ref, if so, store its Identifier reference if an instruction is generated here
        Integer op1IdRef = checkVarRefFactor(peek());
        Integer op2IdRef;
        op1 = res = factor();
        while (checkIfTokenIs(peek(), "*") || checkIfTokenIs(peek(), "/") ) {
            Token sym = next();     // consumes "*" or "/"
            op2IdRef = checkVarRefFactor(peek());
            op2 = factor();
            if (checkIfTokenIs(sym, "*")) {
                res = new BinaryInstr(Instruction.Op.MUL, op1, op2);
            } else {
                res = new BinaryInstr(Instruction.Op.DIV, op1, op2);
            }
            ((BinaryInstr)res).setOpIdReferences(op1IdRef, op2IdRef);
            IR.insertInstrToCurrentBlock(res);
            op1 = res;
            op1IdRef = null;
            termIsVarRef = false;
        }
        return res;
    }

    /**  */
    private Integer checkVarRefFactor(Token peek) {
        if (peek.isUserDefinedIdentifier()) {
            termIsVarRef = true;
            termVarRefId = peek.getIdValue();
            return peek.getIdValue();
        }
        else {
            return null;
        }
    }

    /** expression() returns the Instruction object that is the value of the expressionn */
    public Instruction expression() {
        // DONE
        System.out.println("expression");
        Instruction op1, op2, res;
        op1 = res = term();
        // check if expression is expression term is a varRef
        Integer op1IdRef = checkVarRefTerm();
        Integer op2IdRef;

        while (checkIfTokenIs(peek(), "+") || checkIfTokenIs(peek(), "-") ) {
            Token sym = next();
            op2 = term();
            op2IdRef = checkVarRefTerm();
            if (checkIfTokenIs(sym, "+")) {
                res = new BinaryInstr(Instruction.Op.ADD, op1, op2);
            } else {
                res = new BinaryInstr(Instruction.Op.SUB, op1, op2);
            }
            ((BinaryInstr)res).setOpIdReferences(op1IdRef, op2IdRef);
            IR.insertInstrToCurrentBlock(res);
            op1 = res;
            op1IdRef = null;
            exprIsVarRef = false;
        }
        return res;
    }

    private Integer checkVarRefTerm() {
        if (termIsVarRef) {
            termIsVarRef = false;
            exprIsVarRef = true;
            exprVarRefId = termVarRefId;
            return termVarRefId;
        }
        else {
            return null;
        }
    }

    /** relation() does not return any Instruction, it adds cmp & bra instructions to the right block,
     *  the correct block is already entered in body of ifStatement() & whileStatement() */
    public void relation() {
        // DONE
        System.out.println("relation");
        Instruction expr1 = expression();
        Integer op1IdRef = checkVarRefExpr();
        Integer op2IdRef;
        System.out.println("relational comparison: " + lexer.debugToken(peek()));
        if (peek().isRelationalOp()) {
            Token relOp = next();         // consumes relOp
            Instruction expr2 = expression();
            op2IdRef = checkVarRefExpr();
            BinaryInstr cmpInstr = new BinaryInstr(Instruction.Op.CMP, expr1, expr2);
            cmpInstr.setOpIdReferences(op1IdRef, op2IdRef);
            IR.insertInstrToCurrentBlock( cmpInstr );
            IR.insertInstrToCurrentBlock( computeRelOpBranchInstr(relOp, expr1, expr2) );
        } else {
            error("Invalid relation");
        }
    }

    private Integer checkVarRefExpr() {
        if (exprIsVarRef) {
            exprIsVarRef = false;
            return exprVarRefId;
        }
        else {
            return null;
        }
    }

    public void assignment() {
        System.out.println("assignment");
        if (checkIfTokenIs(peek(), "let")) {
            next();     // consumes "let"
            Token var = next();     // consumes identifier
            next();     // consumes "<-"
            Instruction value = expression();
            IR.assign(var.getIdValue(), value);
            System.out.println("variable assigned: " + lexer.debugToken(var));
        }
        else {
            error("assignment does not start with 'let'");
        }
    }

    public Instruction nonVoidFunctionCall() {
        System.out.println("Non-void function call");
        next();     // consumes "call"
        Token funcName = next();
        if (checkIfTokenIs(funcName, "InputNum")) {
            next();
            next();
            Instruction toAdd = new Instruction(Instruction.Op.READ);
            IR.insertInstrToCurrentBlock(toAdd);
            return toAdd;
        }
        else {
            // user defined functions, check for non-void
            // checkNonVoid()
            // return functionCall(). have functionCall() return Instruction, voidFunctionCall can just not use it.
            return null;
        }
    }

    public void voidFunctionCall() {
        System.out.println("void function call");
        next();     // consumes "call"
        Token funcName = next();
        if (checkIfTokenIs(funcName, "OutputNum")) {
            next();     // consumes "("
            Token arg = next();
            next();     // consumes ")"
            UnaryInstr toAdd = new UnaryInstr(Instruction.Op.WRITE, IR.getIdentifierInstruction(arg.getIdValue()));
            toAdd.setOpIdReference(arg.getIdValue());
            IR.insertInstrToCurrentBlock(toAdd);
        }
        else if (checkIfTokenIs(funcName, "OutputNewLine")) {
            next();
            next();
            Instruction toAdd = new Instruction(Instruction.Op.WRITENL);
            IR.insertInstrToCurrentBlock(toAdd);
        }
        else {
            // user defined void function
            // checkVoid()
            functionCall();
        }
    }

    public void functionCall() {
        System.out.println("function call");
        next();     // consumes call
        Token funcName = next();     // consumes identifier (function symbol)
        if (checkIfTokenIs(peek(), "(")) {
            next();
            if (!checkIfTokenIs(peek(), ")")) {
                // replace body w helper function mapArguments()
                expression();
                while (checkIfTokenIs(peek(), ",")) {
                    next();
                    expression();
                }
            }
            next();
        } else {
            if (!functionHasNoParams(funcName)) {       // error if function has parameters but no arguments
                error("invalid function call, function called with no arguments");
            }
        }
    }

    // ONLY propoagate at the end of traversing if-then-else
    public void ifStatement() {
        // DONE
        System.out.println("if statement");
        next();     // consumes "if"
        BasicBlock current = IR.enterIf();      // current = ifBlock
        relation();                        // cmp instructions get added to the current block
        next();     // consumes "then"
        IR.setCurrentBlock(current.getFallThruTo());      // set current to current's then-block
        statementSequence();
        if (checkIfTokenIs(peek(), "else")) {
            System.out.println("else");
            next();
            IR.setCurrentBlock(current);
            IR.generateElseBlock();
            IR.setCurrentBlock(current.getBranchTo());       // set current to current's else-block
            statementSequence();
        }
        next();     // consumes "fi"
        IR.setCurrentBlock(IR.findJoinBlock());             // set current to current's join
        IR.propagateNestedIf(current);                           // propagate phi, the join block should only have phi functions
        if (!IR.getCurrentBlock().isNested()) {
            // if currentBlock is un-nested, update its Symbol Table to have all identifiers mapped to correct values
            IR.getCurrentBlock().updateSymbolTableFromParent(current);
        }

    }

    public void whileStatement() {
        // DONE
        System.out.println("while statement");
        next();     // consumes "while"
        BasicBlock current = IR.enterWhile();       // current = whileBlock
        relation();         // cmp instructions get added to while-block
        next();     // consumes "do"
        IR.setCurrentBlock(current.getFallThruTo());        // current = while-body
        statementSequence();
        // need to assign branch target later !!!!
        IR.insertInstrToCurrentBlock(new UnaryInstr(Instruction.Op.BRA, null));
        next();     // consumes "od"
        IR.setCurrentBlock(current.getBranchTo());          // current = while-follow
        IR.propagateNestedIf(current);           // propagate if nested
        // if whileFollow is un-nested, update its Symbol Table to have all updated variable values
        if (!IR.getCurrentBlock().isNested()) {
            IR.getCurrentBlock().updateSymbolTableFromParent(current);
        }
        // propagate while
    }

    public void returnStatement() {
        System.out.println("return statement");
        next();     // consumes "return"
        Token peek = peek();
        if (peek.isUserDefinedIdentifier() || peek.isLiteral() ||
                checkIfTokenIs(peek, "(") || checkIfTokenIs(peek, "call")) {
            expression();
        }
    }

    public void statement() {
        System.out.println("statement");
        Token peek = peek();
        if (checkIfTokenIs(peek, "let")) {
            assignment();
        } else if (checkIfTokenIs(peek, "call")) {
            voidFunctionCall();
        } else if (checkIfTokenIs(peek, "if")) {
            ifStatement();
        } else if (checkIfTokenIs(peek, "while")) {
            whileStatement();
        } else if (checkIfTokenIs(peek, "return")) {
            returnStatement();
        } else {
            error("invalid statement");
        }
    }

    public void statementSequence() {
        System.out.println("statement sequence");
        statement();
        while (checkIfTokenIs(peek(), ";")) {
            next();
            if (checkIfTokenIs(peek(), "let") || checkIfTokenIs(peek(), "call") ||
                    checkIfTokenIs(peek(), "if") || checkIfTokenIs(peek(), "while") ||
                    checkIfTokenIs(peek(), "return"))
            {
                statement();
            }
        }
    }

    /** add every var-declaration to the current block's symbol table, initialize to null */
    public void variableDeclaration() {
        // DONE
        System.out.println("variable declaration");
        next();     // consumes "var"
        Token var = next();     // consumes identifier
        IR.addVarDecl(var.getIdValue());     // add identifier id to symbol table
        System.out.println("variable declared: " + lexer.debugToken(var));
        while (checkIfTokenIs(peek(), ",")) {
            next();     // consumes ","
            var = next();     // consumes identifier
            IR.addVarDecl(var.getIdValue());     // add identifier id to symbol table
            System.out.println("variable declared: " + lexer.debugToken(var));
        }
        next();     // consumes ';'
    }

    public void functionDeclaration() {
        System.out.println("function declaration");
        if (checkIfTokenIs(peek(), "void")) {
            next();
        }
        next();     // consumes "function"
        next();     // consumes identifier
        formalParameters();
        next();     // consumes ";"
        functionBody();
        next();     // consumes ";"
    }

    public void formalParameters() {
        System.out.println("formal parameters");
        next();     // consumes "("
        if (peek().isUserDefinedIdentifier()) {
            next();         // consumes identifier
            while (checkIfTokenIs(peek(), ",")) {
                next();
                next();     // consumes identifiier
            }
        }
        next();     // consumes ")"
    }

    public void functionBody() {
        System.out.println("function body");
        while (checkIfTokenIs(peek(), "var")) {
            variableDeclaration();
        }
        next();     // consumes "{"
        if (checkIfTokenIs(peek(), "let") || checkIfTokenIs(peek(), "call") ||
                checkIfTokenIs(peek(), "if") || checkIfTokenIs(peek(), "while") ||
                checkIfTokenIs(peek(), "return"))
        {
            statementSequence();
        }
        next();     // consumes "}"
    }

    public void computation() {
        System.out.println("computation");
        next();     // consumes "main"
        IR.generateFallThruBlock(BasicBlock.BlockType.BASIC);      // generate new block for varDecl (linear, no branches)
        while (checkIfTokenIs(peek(), "var")) {
            variableDeclaration();
        }
        while (checkIfTokenIs(peek(), "void") || checkIfTokenIs(peek(), "function")) {
            functionDeclaration();
        }
        next();     // consumes "{"
        statementSequence();
        next();     // consumes "}"
        next();     // consumes "."
        if (peek() == null) {
            System.out.println("DONE PARSING!");
        }
        IR.printCFG();
        IR.printSymbolTable(lexer.getIdentifiersMappedToId());
    }

    // ------------ HELPER FUNCTIONS ------------- //
    private Instruction computeRelOpBranchInstr(Token relOp, Instruction expr1, Instruction expr2) {
        // target Instruction has to be updated later else/join block has been generated
        if (checkIfTokenIs(relOp, "==")) {
            return new UnaryInstr(Instruction.Op.BNE, null);
        }
        else if (checkIfTokenIs(relOp, "!=")) {
            return new UnaryInstr(Instruction.Op.BEQ, null);
        }
        else if (checkIfTokenIs(relOp, "<")) {
            return new UnaryInstr(Instruction.Op.BGE, null);
        }
        else if (checkIfTokenIs(relOp, "<=")) {
            return new UnaryInstr(Instruction.Op.BGT, null);
        }
        else if (checkIfTokenIs(relOp, ">")) {
            return new UnaryInstr(Instruction.Op.BLE, null);
        }
        else {      // ">="
            return new UnaryInstr(Instruction.Op.BLT, null);
        }
    }

    private boolean functionHasNoParams(Token funcName) {
        return true;
    }

    private boolean checkIfTokenIs(Token token, String sym) {
        return token.getIdValue() == this.lexer.getIdentifierID(sym) || token.getIdValue() == this.lexer.getSymbolID(sym);
    }

    private Token peek() {      // looks at next token without consuming. Null at EOF.
        return this.lexer.peek();
    }

    private Token next() {      // consumes next token
        try {
            return this.lexer.next();
        } catch (IOException e) {
            System.out.println("frontend.Parser next(): IO error");
        } catch (TinySyntaxError e) {
            System.out.println("frontend.Parser next(): tiny syntax error");
        }
        return null;
    }

    private void error(String message) {
        System.out.println("SYNTAX ERROR: " + message);
    }

    private void warning(String message) {
        System.out.println("COMPILE WARNING: " + message);
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("tests/SSA/while-propagation-test.tiny");
        Parser parser = new Parser(lexer);
    }

}
