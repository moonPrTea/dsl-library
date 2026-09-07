package moon.example;

import moon.example.common.FileInformation;
import moon.example.common.TextRange;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.lexer.Lexer;
import moon.example.lexer.Token;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    Path path = (args.length > 0) ? Path.of(args[0])
            : Path.of("examples/example.bck");

    FileInformation file = FileInformation.readFrom(path);
    ProblemCollector problems = new ProblemCollector(file);

    problems.add(Problem.error(ProblemCode.UNKNOWN_CHARACTER, new TextRange(1, 3),
            "unknown character '@'"));

    problems.add(Problem.error(ProblemCode.UNTERMINATED_STRING, new TextRange(4, 8),
            "unterminated string literal: reached end of line",
            "add a closing quote"));

    List<Token> tokens = new Lexer(file, problems).tokenizeFile();
    for (Token token : tokens) {
      System.out.println(token);
    }

    System.err.print(problems.renderProblems());
    System.exit(problems.hasAnyErrors() ? 1 : 0);


    System.err.print(problems.renderProblems());
    System.exit(problems.hasAnyErrors() ? 1 : 0);
  }
}
