import errors.TinySyntaxError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** NOTES: Lexer class parses input source program char by char
 *         calling .peek() returns the next token WITHOUT consuming it, calling .next() consumes the next token
 *         Identifier and Symbol names/string representations are entirely contained within the Lexer class.
 *         Output Tokens are identified by an int ID in the symbol/identifier table inside the Lexer.
 *      **/
public class Lexer {

    private final Map<String, Integer> symbolMappedToId = new HashMap<>();
    private final Map<String, Integer> identifiersMappedToId = new HashMap<>();

    private final BufferedReader reader;
    private boolean lookedAhead = false;        // manual 1 char lookahead. So reader.reset() can be used for token look ahead.
    private int currentChar;

    public Lexer(String fileName) {
        initializeKeywordIdentifiers();
        symbolMappedToId();
        try {
            File file = new File(fileName);
            this.reader = new BufferedReader(new FileReader(file));
        } catch (IOException e){
            throw new RuntimeException("IOException thrown: " + fileName + " cannot be opened.");
        }
    }

    // RETURNS NEXT TOKEN IN INPUT SOURCE PROGRAM
    public Token next() throws IOException, TinySyntaxError {
        if (!this.lookedAhead) {
            this.currentChar = this.reader.read();
        } else {
            this.lookedAhead = false;
        }
        if (this.currentChar == -1) {
            return null;
        }
        // CHECK IF SYMBOL
        switch (this.currentChar) {
            case '(':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("("));
            case ')':
                return new Token(Token.TokenType.SYMBOL, getSymbolID(")"));
            case '+':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("+"));
            case '-':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("-"));
            case '*':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("*"));
            case '/':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("/"));
            case ';':
                return new Token(Token.TokenType.SYMBOL, getSymbolID(";"));
            case ',':
                return new Token(Token.TokenType.SYMBOL, getSymbolID(","));
            case '{':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("{"));
            case '}':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("}"));
            case '.':
                return new Token(Token.TokenType.SYMBOL, getSymbolID("."));
            case '=':
                if ((this.currentChar = this.reader.read()) == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("=="));
                } else {
                    throw new TinySyntaxError("SYNTAX ERROR DETECTED: =" + (char)this.currentChar + " IS NOT A VALID SYMBOL");
                }
            case '!':
                if ((this.currentChar = this.reader.read()) == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("!="));
                } else {
                    throw new TinySyntaxError("SYNTAX ERROR DETECTED: !" + (char)this.currentChar + " IS NOT A VALID SYMBOL");
                }
            case '<':
                this.currentChar = this.reader.read();      // read next character
                if (this.currentChar == '-') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<-"));
                }
                else if (this.currentChar == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<="));
                }
                else {
                    this.lookedAhead = true;
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<"));
                }
            case '>':
                this.currentChar = this.reader.read();
                if (this.currentChar == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID(">="));
                }
                else {
                    this.lookedAhead = true;
                    return new Token(Token.TokenType.SYMBOL, getSymbolID(">"));
                }
        }
        // CHECK LITERAL VALUE
        if (isDigit(this.currentChar)) {
            int value = 0;
            do {
                value = value * 10 + (this.currentChar - 48);
            }
            while ( isDigit(this.currentChar = this.reader.read()) );

            this.lookedAhead = true;
            return new Token(Token.TokenType.LITERAL, value);
        }
        // CHECK IDENTIFIER
        else if (isLetter(this.currentChar)) {
            StringBuilder name = new StringBuilder();
            do {
                name.append((char)this.currentChar);
                this.currentChar = this.reader.read();
            }
            while (isDigit(this.currentChar) || isLetter(this.currentChar));

            this.lookedAhead = true;
            if (getIdentifierID(name.toString()) == -1) {
                int id = this.identifiersMappedToId.size()+1;
                this.identifiersMappedToId.put(name.toString(), id);
                return new Token(Token.TokenType.IDENTIFIER, id);
            }
            else {
                return new Token( Token.TokenType.IDENTIFIER, getIdentifierID(name.toString()) );
            }
        }
        else if (isWhiteSpace(this.currentChar)) {
            return next();
        }
        else {
            throw new TinySyntaxError("Tiny Syntax Error!");
        }
    }

    public Token peek() {
        try {
            this.reader.mark(26);
            boolean saveLookedAhead = this.lookedAhead;
            int saveCurrentChar = this.currentChar;

            Token res = next();
            this.reader.reset();
            // restore all states after reset bufferedReader.
            this.lookedAhead = saveLookedAhead;
            this.currentChar = saveCurrentChar;
            return res;
        }
        catch (IOException e) {
            System.out.println("IO error");
        } catch (TinySyntaxError e) {
            System.out.println("tiny syntax error");
        }
        return null;
    }

    public int getSymbolID(String symbol) {
        return this.symbolMappedToId.getOrDefault(symbol, -1);
    }

    public int getIdentifierID(String keyword) {
        return this.identifiersMappedToId.getOrDefault(keyword, -1);
    }

    public String getIdentiferName(int id) {
        for (Map.Entry<String, Integer> set : this.identifiersMappedToId.entrySet()) {
            if (set.getValue() == id) {
                return set.getKey();
            }
        }
        return null;
    }

    private boolean isDigit(int c) {
        return c >= 48 && c <= 57;
    }

    private boolean isLetter(int c) {
        return c >= 97 && c <= 122 || c >= 65 && c <= 90;
    }

    private boolean isWhiteSpace(int c) {
        return c == ' ' || c == '\n' || c == '\t' || c == '\f' || c == '\r';
    }

    private void symbolMappedToId() {
        int id = 1;
        this.symbolMappedToId.put("==", id++);      // 1
        this.symbolMappedToId.put("!=", id++);
        this.symbolMappedToId.put("<", id++);       // 3
        this.symbolMappedToId.put("<=", id++);
        this.symbolMappedToId.put(">", id++);       // 5
        this.symbolMappedToId.put(">=", id++);
        this.symbolMappedToId.put("(", id++);       // 7
        this.symbolMappedToId.put(")", id++);
        this.symbolMappedToId.put("+", id++);       // 9
        this.symbolMappedToId.put("-", id++);
        this.symbolMappedToId.put("*", id++);       // 11
        this.symbolMappedToId.put("/", id++);
        this.symbolMappedToId.put("<-", id++);      // 13
        this.symbolMappedToId.put(";", id++);
        this.symbolMappedToId.put(",", id++);       // 15
        this.symbolMappedToId.put("{", id++);
        this.symbolMappedToId.put("}", id++);       // 17
        this.symbolMappedToId.put(".", id);
    }

    private void initializeKeywordIdentifiers() {
        int id = 1;
        this.identifiersMappedToId.put("let", id++);        // 1
        this.identifiersMappedToId.put("call", id++);
        this.identifiersMappedToId.put("if", id++);         // 3
        this.identifiersMappedToId.put("then", id++);
        this.identifiersMappedToId.put("else", id++);       // 5
        this.identifiersMappedToId.put("fi", id++);
        this.identifiersMappedToId.put("while", id++);      // 7
        this.identifiersMappedToId.put("do", id++);
        this.identifiersMappedToId.put("od", id++);         // 9
        this.identifiersMappedToId.put("return", id++);
        this.identifiersMappedToId.put("var", id++);        // 11
        this.identifiersMappedToId.put("void", id++);
        this.identifiersMappedToId.put("function", id++);   // 13
        this.identifiersMappedToId.put("main", id);
    }


    // FOR DEBUGGING PURPOSES ONLY
    public String debugToken(Token token) {
        if (token.isLiteral()) {
            return String.format("Literal: %d\n", token.value);
        }
        else if (token.isSymbol()) {
            for (Map.Entry<String, Integer> entry : this.symbolMappedToId.entrySet()) {
                if (entry.getValue() == token.value) {
                    return "Symbol: " + entry.getKey() + " | id: " + token.value;
                }
            }
        }
        else {
            for (Map.Entry<String, Integer> entry : this.identifiersMappedToId.entrySet()) {
                if (entry.getValue() == token.value) {
                    return "Identifier: " + entry.getKey() + " | id: " + token.value;
                }
            }
        }
        return "token id not found";
    }


    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer("tests/while-if-if.tiny");
            Token next;
            while ((next = lexer.peek()) != null) {
                next = lexer.peek();
                next = lexer.peek();
                lexer.next();
                System.out.println(lexer.debugToken(next));
            }
        } catch (IOException e) {
            System.out.println("IO error");
        } catch (TinySyntaxError e) {
            System.out.println("tiny syntax error");
        }
    }
}
