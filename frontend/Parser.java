package frontend;

import IR.BasicBlock.BasicBlock;
import IR.Instruction.Instruction;
import IR.Instruction.BinaryInstr;
import IR.Instruction.MjuInstr;
import IR.Instruction.UnaryInstr;
import IR.SSAIR.GlobalSSAIR;
import errors.TinySyntaxError;

import java.io.IOException;

/** A recursive descent parser based on EBNF for tiny. SSA IR is generated while parsing. */
public class Parser {

    private final Lexer lexer;
    private final GlobalSSAIR GlobalIR;

    // helper variables for mapping identifier to instruction operands
    private boolean termIsVarRef = false;
    private Integer termVarRefId;
    private boolean exprIsVarRef = false;
    private Integer exprVarRefId;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.GlobalIR = new GlobalSSAIR();
    }

    public void parse() {
        computation();
    }

    /** returns the Instruction that is mapped to the variable */
    public Instruction variableReference() {
        System.out.println("variable reference: " + this.lexer.debugToken(peek()));
        Token var = next();
        Instruction value = GlobalIR.getIdentifierInstruction(var.getIdValue());
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
        return GlobalIR.addConstantIfNotExists(var.getIdValue());
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
            op1 = res = GlobalIR.insertInstrToCurrentBlock(res);
            op1IdRef = null;
            termIsVarRef = false;
        }
        return res;
    }

    /** returns id of identifier if current factor is referring to an identifier */
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

    /** expression() returns the Instruction object that is the value of the expression */
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
            op1 = res = GlobalIR.insertInstrToCurrentBlock(res);
            op1IdRef = null;
            exprIsVarRef = false;
        }
        return res;
    }

    /** returns id of identifier if current expression is referring to an identifier */
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
            GlobalIR.insertInstrToCurrentBlock( cmpInstr );
            GlobalIR.insertInstrToCurrentBlock( computeRelOpBranchInstr(relOp) );
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
            GlobalIR.assign(var.getIdValue(), value);
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
            GlobalIR.insertInstrToCurrentBlock(toAdd);
            return toAdd;
        }
        else {
            if (!GlobalIR.functionIsVoid(funcName.getIdValue())) {
                // return functionCall(). have functionCall() return Instruction, voidFunctionCall can just not use it.
                // mju instr for return value
                Instruction res = functionCall(funcName);
                GlobalIR.insertInstrToCurrentBlock(new MjuInstr(res, 30));
                return res;
            }
            else {
                error("Invalid function call, expected non-void function call.");
                return null;
            }
        }
    }

    public void voidFunctionCall() {
        System.out.println("void function call");
        next();     // consumes "call"
        Token funcName = next();
        if (checkIfTokenIs(funcName, "OutputNum")) {
            next();     // consumes "("
            Instruction arg = expression();
            next();     // consumes ")"
            UnaryInstr toAdd = new UnaryInstr(Instruction.Op.WRITE, arg);
            toAdd.setOpIdReference(checkVarRefExpr());
            GlobalIR.insertInstrToCurrentBlock(toAdd);
        }
        else if (checkIfTokenIs(funcName, "OutputNewLine")) {
            next();
            next();
            Instruction toAdd = new Instruction(Instruction.Op.WRITENL);
            GlobalIR.insertInstrToCurrentBlock(toAdd);
        }
        else if (GlobalIR.functionIsVoid(funcName.getIdValue())) {
            System.out.println("function is void, function call is VALID!");
            functionCall(funcName);             // user defined void function
        }
        else {
            error("invalid function call, function called here must be void.");
        }
    }

    public Instruction functionCall(Token funcName) {
        System.out.println("function call");
        Instruction arg;

        int rgId = 0;
        if (checkIfTokenIs(peek(), "(")) {
            next();         // consumes "("
            if (!checkIfTokenIs(peek(), ")")) {
                arg = expression();
                GlobalIR.insertInstrToCurrentBlock(new MjuInstr(arg, ++rgId));
                while (checkIfTokenIs(peek(), ",")) {
                    next();
                    arg = expression();
                    GlobalIR.insertInstrToCurrentBlock(new MjuInstr(arg, ++rgId));
                }
            }
            next();         // consumes ")"
            return GlobalIR.callCurrentFunction(funcName.getIdValue(), lexer.getIdentifierName(funcName.getIdValue()));
        } else {
            if (!functionHasNoParams(funcName)) {       // error if function has parameters but no arguments
                error("invalid function call, function called with no arguments");
                return null;
            } else {
                // call function w no arguments
                return GlobalIR.callCurrentFunction(funcName.getIdValue(), lexer.getIdentifierName(funcName.getIdValue()));
            }
        }
    }

    // DONE
    public void ifStatement() {
        System.out.println("if statement");
        next();                                          // consumes "if"
        BasicBlock parent = GlobalIR.enterIf();                // save parent ifBlock
        BasicBlock join = parent.getBranchTo();          // save join block
        relation();                                      // cmp instructions get added to the parent block
        next();                                          // consumes "then"
        GlobalIR.setCurrentBlock(parent.getFallThruTo());      // current = then-block
        statementSequence();
        if (checkIfTokenIs(peek(), "else")) {
            System.out.println("else");
            next();                                             // consumes "else"
            GlobalIR.setCurrentBlock(GlobalIR.generateElseBlock(parent));   // current = elseBlock
            statementSequence();
            GlobalIR.addBranchInstr(join.getBranchFrom());            // adds branch instruction from last block in if-then branch to if-join
        }
        GlobalIR.setBranchInstr(parent);                      // set cmp branch instr after generating then/else/join
        next();                                         // consumes "fi"
        GlobalIR.setCurrentBlock(GlobalIR.findJoinBlock());         // current = join
        GlobalIR.propagateNestedIf(parent);                   // propagate phi, the join block should only have phi functions
        // if currentBlock is un-nested, update its Symbol Table to have all identifiers mapped to correct values
        if (!GlobalIR.getCurrentBlock().isNested()) {
            GlobalIR.getCurrentBlock().updateSymbolTableFromParent(parent);
        }
    }

    // DONE
    public void whileStatement() {
        System.out.println("while statement");
        next();                                     // consumes "while"
        BasicBlock parent = GlobalIR.enterWhile();        // parent = whileBlock
        relation();                                 // cmp instructions get added to while-block
        next();                                     // consumes "do"
        GlobalIR.setCurrentBlock(parent.getFallThruTo()); // current = while-body
        statementSequence();
        GlobalIR.addBranchInstr(GlobalIR.getCurrentBlock());    // adds branch instruction from while-body to parent-while
        GlobalIR.setBranchInstr(parent);                  // updates operand of parent block's last branch instruction
        next();                                     // consumes "od"
        GlobalIR.setCurrentBlock(parent);                 // set currentBlock to block w phi's, for helper functions
        GlobalIR.propagateNestedWhile(parent);
        GlobalIR.setCurrentBlock(parent.getBranchTo());   // parent = while-follow
        // if whileFollow is un-nested, update its Symbol Table to have all updated variable values
        if (!GlobalIR.getCurrentBlock().isNested()) {
            GlobalIR.getCurrentBlock().updateSymbolTableFromParent(parent);
        }
    }

    public void returnStatement() {
        System.out.println("return statement");
        next();     // consumes "return"
        Token peek = peek();
        if (peek.isUserDefinedIdentifier() || peek.isLiteral() ||
                checkIfTokenIs(peek, "(") || checkIfTokenIs(peek, "call")) {
            Instruction res = expression();
            UnaryInstr ret = new UnaryInstr(Instruction.Op.RET, res);
            GlobalIR.insertInstrToCurrentBlock(ret);
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
        GlobalIR.addVarDecl(var.getIdValue());     // add identifier id to symbol table
        System.out.println("variable declared: " + lexer.debugToken(var));
        while (checkIfTokenIs(peek(), ",")) {
            next();     // consumes ","
            var = next();     // consumes identifier
            GlobalIR.addVarDecl(var.getIdValue());     // add identifier id to symbol table
            System.out.println("variable declared: " + lexer.debugToken(var));
        }
        next();     // consumes ';'
    }

    public void functionDeclaration() {
        System.out.println("function declaration");
        GlobalIR.enterFunctionDef();
        if (checkIfTokenIs(peek(), "void")) {
            next();     // consumes "void"
            GlobalIR.currentFunctionIsVoid();
        }
        next();     // consumes "function"
        Token funcIdent = next();     // consumes identifier
        GlobalIR.setCurrentFunctionIdent(funcIdent.getIdValue());       // save function name identifier
        formalParameters();
        next();     // consumes ";"
        functionBody();
        next();     // consumes ";"
        GlobalIR.restoreGlobalIR();          // restore global CFG
    }

    public void formalParameters() {
        System.out.println("formal parameters");
        next();     // consumes "("
        if (peek().isUserDefinedIdentifier()) {
            Token param = next();         // consumes identifier
            GlobalIR.addParamToCurrentFunction(param.getIdValue());
            while (checkIfTokenIs(peek(), ",")) {
                next();     // consumes ","
                param = next();     // consumes identifier
                GlobalIR.addParamToCurrentFunction(param.getIdValue());
            }
        }
        next();     // consumes ")"
    }

    public void functionBody() {
        System.out.println("function body");
        GlobalIR.setCurrentBlock(GlobalIR.generateFallThruBlock(BasicBlock.BlockType.BASIC));      // generate new block for varDecl (linear, no branches)
        GlobalIR.initializeParamsVarDecl();     // assigns params to argument registers
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
        GlobalIR.setCurrentBlock(GlobalIR.generateFallThruBlock(BasicBlock.BlockType.BASIC));      // generate new block for varDecl (linear, no branches)
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
        if (!GlobalIR.error()) {
            GlobalIR.propagateCommonSubexpr();
        }
        if (peek() == null) {
            System.out.println("DONE PARSING!\n");
        }
        for (Integer var : GlobalIR.getUninitializedVarErrors()) {
            System.out.printf("ERROR: VARIABLE %s NOT INITIALIZED ON ALL PATHS\n", lexer.getIdentifierName(var));
        }
        GlobalIR.printCFG(lexer.getIdentifiersMappedToId(), true);
    }


    // ------------ HELPER FUNCTIONS ------------- //

    private Instruction computeRelOpBranchInstr(Token relOp) {
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


    // ------------------------------- MAIN -------------------------------- //

    public static void main(String[] args) {
        Lexer lexer = new Lexer("tests/peers/test99.tiny");
        Parser parser = new Parser(lexer);
        parser.parse();
    }
}
