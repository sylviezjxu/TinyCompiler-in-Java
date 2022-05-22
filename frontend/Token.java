package frontend;// TOKEN class
// A token can be SYMBOL, IDENTIFIER, VALUE
// A token has an int ID to tell which Symbol/Identifier it is.

public class Token {

    private TokenType tokenType;      // SYMBOL, IDENTIFIER, LITERAL
    private final int idOrValue;                // Id for symbol & identifier, value for literal

    public enum TokenType {
        SYMBOL,
        IDENTIFIER,
        LITERAL
    }

    public Token(TokenType tokenType, int value) {
        this.tokenType = tokenType;
        this.idOrValue = value;
    }

    public int getIdValue() {
        return this.idOrValue;
    }

    public boolean isSymbol() { return this.tokenType.compareTo(TokenType.SYMBOL) == 0; }

    public boolean isLiteral() { return this.tokenType.compareTo(TokenType.LITERAL) == 0; }

    public boolean isUserDefinedIdentifier() {
        return this.tokenType.compareTo(TokenType.IDENTIFIER) == 0 && this.idOrValue > 14;
    }

    public boolean isRelationalOp() {
        return this.tokenType.compareTo(TokenType.SYMBOL) == 0 && this.idOrValue >= 1 && this.idOrValue <= 6;
    }

    public static void main(String[] args) {
    }
}
