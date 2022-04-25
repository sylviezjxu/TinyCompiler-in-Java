import IR.SSAIR;
import errors.TinySyntaxError;

import java.io.IOException;

/** A recursive descent parser based on EBNF for tiny. SSA IR is generated while parsing. */
public class Parser {

    private final Lexer lexer;
    private final SSAIR IR;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        this.IR = new SSAIR();
        computation();
    }

    public void variableReference() {
        System.out.println("variable reference: " + this.lexer.debugToken(peek()));
        next();
    }

    public void number() {
        System.out.println("number" + this.lexer.debugToken(peek()));
        next();
    }

    public void factor() {
        System.out.println("factor");
        Token peek = peek();
        if (peek.isUserDefinedIdentifier()) {
            variableReference();
        }
        else if (peek.isLiteral()) {
            number();
        }
        else if (checkIfTokenIs(peek, "(")) {
            next();
            expression();
            if (checkIfTokenIs(peek(), ")")) {
                next();
            } else {
                error("Factor expression missing ')'");
            }
        }
        else if (checkIfTokenIs(peek, "call")) {
            nonVoidFunctionCall();
        }
    }

    public void term() {
        System.out.println("term");
        factor();
        while (checkIfTokenIs(peek(), "*") || checkIfTokenIs(peek(), "/") ) {
            next();
            factor();
        }
    }

    public void expression() {
        System.out.println("expression");
        term();
        while (checkIfTokenIs(peek(), "+") || checkIfTokenIs(peek(), "-") ) {
            next();
            term();
        }
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
            expression();
        }
        else {
            error("assignment does not start with 'let'");
        }
    }

    public void nonVoidFunctionCall() {
        System.out.println("Non-void function call");
        functionCall();
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
        return token.getId() == this.lexer.getIdentifierID(sym) || token.getId() == this.lexer.getSymbolID(sym);
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

    public static void main(String[] args) {
        Lexer lexer = new Lexer("tests/while-if-if.tiny");
        Parser parser = new Parser(lexer);
    }

}
