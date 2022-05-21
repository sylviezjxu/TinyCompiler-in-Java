import frontend.Lexer;
import frontend.Parser;

public class TinyCompiler {

    public static void Compile(String fileName) {
        Lexer lexer = new Lexer(fileName);
        Parser parser = new Parser(lexer);
        parser.parse();
    }

    public static void main(String[] args) {
        TinyCompiler.Compile("tests/CSE/tricky/tricky.tiny");
    }
}
