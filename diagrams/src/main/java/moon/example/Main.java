package moon.example;

import moon.example.ast.AstPrinter;
import moon.example.ast.Program;
import moon.example.common.FileInformation;
import moon.example.diagnostic.ProblemCollector;
import moon.example.lexer.Lexer;
import moon.example.lexer.Token;
import moon.example.parser.Parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    Path path = (args.length > 0) ? Path.of(args[0])
            : Path.of("examples/example.bck");

    FileInformation file = FileInformation.readFrom(path);
    ProblemCollector problems = new ProblemCollector(file);

    List<Token> tokens = new Lexer(file, problems).tokenizeFile();

    if (!problems.hasAnyErrors()) {
      Program program = new Parser(tokens, problems).parseProgram();
      System.out.print(AstPrinter.print(program));
    }

    System.err.print(problems.renderProblems());
    System.exit(problems.hasAnyErrors() ? 1 : 0);
  }
}
