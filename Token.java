// TOKEN class
// A token can be SYMBOL, IDENTIFIER, VALUE
// A token has an int ID to tell which Symbol/Identifier it is.

public class Token {

    TokenType tokenType;      // SYMBOL, IDENTIFIER, LITERAL
    int value;                // Id for symbol & identifier, value for literal

    public enum TokenType {
        SYMBOL,
        IDENTIFIER,
        LITERAL
    }

    public Token(TokenType tokenType, int value) {
        this.tokenType = tokenType;
        this.value = value;
    }

    public int getId() {
        return this.value;
    }

    public boolean isSymbol() {
        return this.tokenType.compareTo(TokenType.SYMBOL) == 0;
    }

    public boolean isIdentifier() {
        return this.tokenType.compareTo(TokenType.IDENTIFIER) == 0 &&
                this.value >= 1 && this.value <= 14;
    }

    public boolean isLiteral() {
        return this.tokenType.compareTo(TokenType.LITERAL) == 0;
    }

    public static void main(String[] args) {
    }
}
