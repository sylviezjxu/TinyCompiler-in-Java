package frontend;// TOKEN class
// A token can be SYMBOL, IDENTIFIER, VALUE
// A token has an int ID to tell which Symbol/Identifier it is.

public class Token {

    private TokenType tokenType;      // SYMBOL, IDENTIFIER, LITERAL
    private final int get;                // Id for symbol & identifier, value for literal

    public enum TokenType {
        SYMBOL,
        IDENTIFIER,
        LITERAL
    }

    public Token(TokenType tokenType, int value) {
        this.tokenType = tokenType;
        this.get = value;
    }

    public int getIdValue() {
        return this.get;
    }

    public boolean isSymbol() { return this.tokenType.compareTo(TokenType.SYMBOL) == 0; }

    public boolean isLiteral() { return this.tokenType.compareTo(TokenType.LITERAL) == 0; }

    public boolean isUserDefinedIdentifier() {
        return this.tokenType.compareTo(TokenType.IDENTIFIER) == 0 && this.get > 14;
    }

    public boolean isRelationalOp() {
        return this.tokenType.compareTo(TokenType.SYMBOL) == 0 && this.get >= 1 && this.get <= 6;
    }

    public static void main(String[] args) {
    }
}
