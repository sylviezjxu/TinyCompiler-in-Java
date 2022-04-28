import IR.BasicBlock;
import IR.Instruction.Instruction;
import IR.Instruction.OpInstruction;
import IR.SSAIR;
import errors.TinySyntaxError;

import java.io.IOException;
import java.util.LinkedList;

/** A recursive descent parser based on EBNF for tiny. SSA IR is generated while parsing. */
public class Parser {

    private final Lexer lexer;
    private final SSAIR IR;

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
        // check if value has been declared/assigned a value
        if (value == null) {
            warning( String.format("variable %s is referenced but never initialized.",
                     lexer.getIdentiferName(var.getIdValue())) );

        }
        return value;
    }

    /** returns the Instruction that represents the value of the literal */
    public Instruction number() {
        System.out.println("number" + this.lexer.debugToken(peek()));
        Token var = next();
        return IR.addConstant(var.getIdValue());
    }

    /** factor() returns the Instruction that represents the value of the var/constant/expression/function call */
    public Instruction factor() {
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
            res = nonVoidFunctionCall();        // returns null rn
        } else {
            res = null;
        }
        return res;
    }

    /** returns the Instruction that represents the value of the term */
    public Instruction term() {
        System.out.println("term");
        Instruction op1, op2, res = null;
        op1 = res = factor();
        while (checkIfTokenIs(peek(), "*") || checkIfTokenIs(peek(), "/") ) {
            Token sym = next();
            op2 = factor();
            if (checkIfTokenIs(sym, "*")) {
                res = new OpInstruction(Instruction.OP.MUL, op1, op2);
                IR.insertInstruction(res);
            } else {
                res = new OpInstruction(Instruction.OP.DIV, op1, op2);
                IR.insertInstruction(res);
            }
            op1 = res;
        }
        return res;
    }

    /** expression() returns the Instruction object that is the value of the expressionn */
    public Instruction expression() {
        System.out.println("expression");
        Instruction op1, op2, res = null;
        op1 = res = term();
        while (checkIfTokenIs(peek(), "+") || checkIfTokenIs(peek(), "-") ) {
            Token sym = next();
            op2 = term();
            if (checkIfTokenIs(sym, "+")) {
                res = new OpInstruction(Instruction.OP.ADD, op1, op2);
                IR.insertInstruction(res);
            } else {
                res = new OpInstruction(Instruction.OP.SUB, op1, op2);
                IR.insertInstruction(res);
            }
            op1 = res;
        }
        return res;
    }

    public void relation() {
        System.out.println("relation");
        expression();
        System.out.println("relational comparison: " + lexer.debugToken(peek()));
        if (peek().isRelationalOp()) {
            next();
            expression();
        } else {
            error("Invalid relation");
        }
    }

    public void assignment() {
        System.out.println("assignment");
        if (checkIfTokenIs(peek(), "let")) {
            next();     // consumes "let"
            Token var = next();     // consumes identifier
            next();     // consumes "<-"
            System.out.println("variable assigned: " + lexer.debugToken(var));
        }
        else {
            error("assignment does not start with 'let'");
        }
    }

    public Instruction nonVoidFunctionCall() {
        System.out.println("Non-void function call");
        functionCall();
        return null;
    }

    public void voidFunctionCall() {
        System.out.println("void function call");
        functionCall();
    }

    public void functionCall() {
        System.out.println("function call");
        next();     // consumes call
        Token funcName = next();     // consumes identifier (function symbol)
        if (checkIfTokenIs(peek(), "(")) {
            next();
            if (!checkIfTokenIs(peek(), ")")) {
                expression();
                while (checkIfTokenIs(peek(), ",")) {
                    next();
                    expression();
                }
            }
            next();
        } else {
            if (!functionHasNoParams(funcName)) {       // error if function has parameters
                error("invalid function call, function called with no arguments");
            }
        }
    }

    public void ifStatement() {
        System.out.println("if statement");
        next();     // consumes "if"
        relation();
        next();     // consumes "then"
        statementSequence();
        if (checkIfTokenIs(peek(), "else")) {
            next();
            statementSequence();
        }
        next();     // consumes "fi"
    }

    public void whileStatement() {
        System.out.println("while statement");
        next();     // consumes "while"
        relation();
        next();     // consumes "do"
        statementSequence();
        next();     // consumes "od"
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

    public void variableDeclaration() {
        System.out.println("variable declaration");
        next();     // consumes "var"
        Token var = next();     // consumes identifier
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
    }

    // ------------ HELPER FUNCTIONS ------------- //
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
            System.out.println("Parser next(): IO error");
        } catch (TinySyntaxError e) {
            System.out.println("Parser next(): tiny syntax error");
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
        Lexer lexer = new Lexer("tests/while-if-if.tiny");
        Parser parser = new Parser(lexer);
    }

}
