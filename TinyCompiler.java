import frontend.Lexer;
import frontend.Parser;

public class TinyCompiler {

    public static void Compile(String fileName) {
        Lexer lexer = new Lexer(fileName);
        Parser parser = new Parser(lexer);
    }

    public static void main(String[] args) {
        TinyCompiler.Compile(args[0]);
    }
}