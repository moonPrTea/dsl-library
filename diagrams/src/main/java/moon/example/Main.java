package moon.example;

import moon.example.analyzer.Analyzer;
import moon.example.ast.Program;
import moon.example.common.FileInformation;
import moon.example.diagnostic.ProblemCollector;
import moon.example.lexer.Lexer;
import moon.example.lexer.Token;
import moon.example.model.Diagram;
import moon.example.model.ModelPrinter;
import moon.example.parser.Parser;
import moon.example.render.DiagramRenderer;
import moon.example.render.RenderScene;
import moon.example.render.SvgDiagramExporter;
import moon.example.specification.SpecificationRegistry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    CommandLine commandLine;

    try {
      commandLine = CommandLine.parse(args);
    } catch (IllegalArgumentException exception) {
      System.err.println(exception.getMessage());
      System.err.print(CommandLine.usage());
      System.exit(2);
      return;
    }

    if (commandLine.helpRequested()) {
      System.out.print(CommandLine.usage());
      return;
    }

    FileInformation file = FileInformation.readFrom(commandLine.input());
    ProblemCollector problems = new ProblemCollector(file);

    List<Token> tokens = new Lexer(file, problems).tokenizeFile();

    if (!problems.hasAnyErrors()) {
      Program program = new Parser(tokens, problems).parseProgram();

      if (!problems.hasAnyErrors()) {
        Diagram diagram = new Analyzer(new SpecificationRegistry(), problems).analyze(program);

        if (diagram != null && !problems.hasAnyErrors()) {
          if (commandLine.renderRequested()) {
            render(diagram, commandLine);
          } else {
            System.out.print(ModelPrinter.print(diagram));
          }
        }
      }
    }

    System.err.print(problems.renderProblems());
    System.exit(problems.hasAnyErrors() ? 1 : 0);
  }

  private static void render(Diagram diagram, CommandLine commandLine) throws IOException {
    RenderScene scene = new DiagramRenderer()
            .render(diagram, commandLine.theme(), commandLine.selectedNode());
    Path output = commandLine.outputPath();

    new SvgDiagramExporter().write(scene, output);
    System.out.println("Rendered " + commandLine.theme().name() + " diagram: " + output);
  }
}
