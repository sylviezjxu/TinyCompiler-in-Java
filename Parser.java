import errors.TinySyntaxError;

import java.io.IOException;

public class Parser {

    private final Lexer lexer;
    private Token currentToken;
    private boolean lookedAhead = false;

    public Parser(Lexer lexer) {
        this.lexer = lexer;
        // start recursive descent
        computation();
    }

    public void variableReference() {
        consumeNextTokenIfNeeded();
        if (!this.currentToken.isIdentifier()) {
            error("invalid variable reference.");
        }
    }

    public void factor() {
        consumeNextTokenIfNeeded();
        if (this.currentToken.isIdentifier()) {
            this.lookedAhead = true;
            // variableReference
        }
        else if (this.currentToken.isLiteral()) {
            this.lookedAhead = true;
            // number
        }
        else if (currentIs("(")) {
            // expression()
            this.currentToken = next();
            if (!currentIs(")")) {
                error("factor expression does not end with closing bracket.");
            }
        }
        else if (currentIs("call")) {
            //nonVoidFunctionCall();
        }
    }

    public void term() {
        factor();
        consumeNextTokenIfNeeded();
        while (currentIs("*") || currentIs("/")) {
            factor();
            consumeNextTokenIfNeeded();
        }
    }

    public void expression() {
        term();
        consumeNextTokenIfNeeded();
        while (currentIs("+") || currentIs("-")) {
            term();
            consumeNextTokenIfNeeded();
        }
    }

    public void relation() {
        expression();                   // expression always looks 1 token ahead to check +/-


    }



    public void computation() {
        this.currentToken = next();     // consumes "main"
        if (currentIs("main")) {
            this.currentToken = next();     // consumes next token
            this.lookedAhead = true;
            while (currentIs("var")) {
                //variableDeclaration();
            }
            while (currentIs("void") || currentIs("function")) {
                //functionDeclaration();
            }
            consumeNextTokenIfNeeded();
            if (currentIs("{")) {
                //statSequence();
            }
            consumeNextTokenIfNeeded();
            if (currentIs("}")) {
                this.currentToken = next();
                if (currentIs(".")) {
                    System.out.println("DONE PARSING!");
                } else {
                    error("'main' does not end with '.'");
                }
            } else {
                error("'main' does not end with '}.'");
            }
        } else {
            error("program does not start with 'main'.");
        }
    }

    private boolean currentIs(String sym) {
        if (this.currentToken.getId() == this.lexer.getIdentifierID(sym) ||
                this.currentToken.getId() == this.lexer.getSymbolID(sym)) {
            this.lookedAhead = false;
            return true;
        } else {
            return false;
        }
    }

    private Token next() {      // returns the next token
        try {
            return this.lexer.next();       // returns null if EOF
        } catch (IOException e) {
            System.out.println("IO error");
        } catch (TinySyntaxError e) {
            System.out.println("tiny syntax error");
        }
        return null;
    }

    private void consumeNextTokenIfNeeded() {
        if (!lookedAhead) {
            this.currentToken = next();
        }
    }

    private void error(String message) {
        System.out.println("SYNTAX ERROR: " + message);
    }

}
