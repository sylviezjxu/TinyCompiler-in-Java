import errors.TinySyntaxError;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** NOTES: Lexer class parses input source program char by char
 *         calling .next() on Lexer object outputs the next Token object in the source program
 *         Identifier and Symbol names/string representations are entirely contained within the Lexer class.
 *         Output Tokens are identified by an int ID in the symbol/identifier table inside the Lexer.
 *      **/
public class Lexer {

    private final Map<String, Integer> symbolMappedToId = new HashMap<>();
    private final Map<String, Integer> identifiersMappedToId = new HashMap<>();

    private final BufferedReader reader;
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
        this.currentChar = this.reader.read();
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
                this.reader.mark(1);    // one character lookahead. Marks current position, reset if token ends at this pos.
                this.currentChar = this.reader.read();      // read next character
                if (this.currentChar == '-') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<-"));
                }
                else if (this.currentChar == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<="));
                }
                else {
                    this.reader.reset();    // reset back one character
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("<"));
                }
            case '>':
                this.reader.mark(1);
                this.currentChar = this.reader.read();
                if (this.currentChar == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID(">="));
                }
                else {
                    this.reader.reset();
                    return new Token(Token.TokenType.SYMBOL, getSymbolID(">"));
                }
        }
        // CHECK LITERAL VALUE
        if (isDigit(this.currentChar)) {
            int value = 0;
            do {
                this.reader.mark(1);
                value = value * 10 + (this.currentChar - 48);
            }
            while ( isDigit(this.currentChar = this.reader.read()) );

            this.reader.reset();
            return new Token(Token.TokenType.LITERAL, value);
        }
        // CHECK IDENTIFIER
        else if (isLetter(this.currentChar)) {
            StringBuilder name = new StringBuilder();
            do {
                this.reader.mark(1);
                name.append((char)this.currentChar);
                this.currentChar = this.reader.read();
            }
            while (isDigit(this.currentChar) || isLetter(this.currentChar));

            this.reader.reset();
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

    public int getSymbolID(String symbol) {
        return this.symbolMappedToId.getOrDefault(symbol, -1);
    }

    public int getIdentifierID(String keyword) {
        return this.identifiersMappedToId.getOrDefault(keyword, -1);
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
//        try {
//            Lexer lexer = new Lexer("tests/while-if-if.tiny");
//            Token next;
//            while ((next = lexer.next()) != null) {
//                System.out.println(lexer.debugToken(next));
//            }
//        } catch (IOException e) {
//            System.out.println("IO error");
//        } catch (TinySyntaxError e) {
//            System.out.println("tiny syntax error");
//        }
    }
}
