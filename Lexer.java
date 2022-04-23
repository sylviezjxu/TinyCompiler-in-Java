import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Lexer {

    private final Map<String, Integer> symbolMappedToId = new HashMap<>();
    private final Map<String, Integer> identifiersMappedToId = new HashMap<>();

    private BufferedReader reader;
    private int currentChar;

    public Lexer(String fileName) {
        initializeKeywords();
        symbolMappedToId();
        try {
            File file = new File(fileName);
            this.reader = new BufferedReader(new FileReader(file));
        } catch (IOException e){
            throw new RuntimeException("IOException thrown: " + fileName + " cannot be opened.");
        }
    }

    public Token next() throws IOException{
        // returns the next TOKEN.
        this.currentChar = this.reader.read();
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
                this.reader.mark(1);        // 1 char lookahead. If char belongs to next token, reader.reset()
                if ((this.currentChar = this.reader.read()) == '=') {
                    return new Token(Token.TokenType.SYMBOL, getSymbolID("=="));
                } else {
                    System.out.println("SYNTAX ERROR DETECTED: =" + (char)this.currentChar + " IS NOT A VALID SYMBOL");
                    this.reader.reset();
                }

        }


        return new Token(Token.TokenType.IDENTIFIER, 1);
    }

    public int getSymbolID(String symbol) {
        return this.symbolMappedToId.getOrDefault(symbol, -1);
    }

    public int getKeywordIdentifierID(String keyword) {
        return this.identifiersMappedToId.getOrDefault(keyword, -1);
    }

    private void symbolMappedToId() {
        int id = 1;
        this.symbolMappedToId.put("==", id++);
        this.symbolMappedToId.put("!=", id++);
        this.symbolMappedToId.put("<", id++);
        this.symbolMappedToId.put("<=", id++);
        this.symbolMappedToId.put(">", id++);
        this.symbolMappedToId.put(">=", id++);
        this.symbolMappedToId.put("(", id++);
        this.symbolMappedToId.put(")", id++);
        this.symbolMappedToId.put("+", id++);
        this.symbolMappedToId.put("-", id++);
        this.symbolMappedToId.put("*", id++);
        this.symbolMappedToId.put("/", id++);
        this.symbolMappedToId.put("<-", id++);
        this.symbolMappedToId.put(";", id++);
        this.symbolMappedToId.put(",", id++);
        this.symbolMappedToId.put("{", id++);
        this.symbolMappedToId.put("}", id++);
        this.symbolMappedToId.put(".", id);
    }

    private void initializeKeywords() {
        int id = 1;
        this.identifiersMappedToId.put("let", id++);
        this.identifiersMappedToId.put("call", id++);
        this.identifiersMappedToId.put("if", id++);
        this.identifiersMappedToId.put("then", id++);
        this.identifiersMappedToId.put("else", id++);
        this.identifiersMappedToId.put("fi", id++);
        this.identifiersMappedToId.put("while", id++);
        this.identifiersMappedToId.put("do", id++);
        this.identifiersMappedToId.put("od", id++);
        this.identifiersMappedToId.put("return", id);
    }

    public static void main(String[] args) {
        try {
            Lexer lexer = new Lexer("while-if-if.tiny");
            lexer.next();
        } catch (IOException e) {
            System.out.println("error");
        }
    }
}
