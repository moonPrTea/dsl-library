package moon.example;

import moon.example.render.DiagramTheme;
import moon.example.render.DiagramThemes;

import java.nio.file.Path;
import java.util.Locale;

public record CommandLine(
        Path input,
        DiagramTheme theme,
        Path output,
        String selectedNode,
        boolean renderRequested,
        boolean pngRequested,
        boolean helpRequested
) {
  private static final Path DEFAULT_INPUT = Path.of("examples/example.bck");
  private static final String SVG_EXTENSION = ".svg";
  private static final String PNG_EXTENSION = ".png";

  public static CommandLine parse(String[] args) {
    Path input = null;
    DiagramTheme theme = DiagramThemes.DEFAULT;
    Path output = null;
    String selectedNode = null;
    boolean renderRequested = false;
    boolean pngRequested = false;
    boolean helpRequested = false;

    for (int i = 0; i < args.length; i++) {
      String argument = args[i];

      switch (argument) {
        case "--theme" -> {
          theme = expectTheme(valueAfter(args, i++));
          renderRequested = true;
        }
        case "--output", "-o" -> {
          output = Path.of(valueAfter(args, i++));
          renderRequested = true;
        }
        case "--selected" -> {
          selectedNode = valueAfter(args, i++);
          renderRequested = true;
        }
        case "--png" -> {
          pngRequested = true;
          renderRequested = true;
        }
        case "--help", "-h" -> helpRequested = true;
        default -> {
          if (argument.startsWith("-")) {
            throw new IllegalArgumentException("unknown option '" + argument + "'");
          }

          if (input != null) {
            throw new IllegalArgumentException("only one input file is expected");
          }

          input = Path.of(argument);
        }
      }
    }

    return new CommandLine(input != null ? input : DEFAULT_INPUT, theme, output, selectedNode,
            renderRequested, pngRequested, helpRequested);
  }

  public static String usage() {
    return """
            Usage: java moon.example.Main [input.bck] [options]
              --theme %s   rendering theme (default: %s)
              --output, -o PATH        output SVG file (default: <input>-<theme>.svg next to the input)
              --selected NODE          render one node as selected
              --png                    also export a PNG next to the SVG
              --help, -h               show this help

            Without rendering options the command prints the analyzed model.
            """.formatted(String.join("|", DiagramThemes.getNames()), DiagramThemes.DEFAULT.name());
  }

  public Path outputPath() {
    if (output == null) {
      String filename = input.getFileName().toString();
      int dot = filename.lastIndexOf('.');
      String stem = dot > 0 ? filename.substring(0, dot) : filename;

      return input.toAbsolutePath().resolveSibling(stem + "-" + theme.name() + SVG_EXTENSION);
    }

    return output.toString().toLowerCase(Locale.ROOT).endsWith(SVG_EXTENSION)
            ? output
            : Path.of(output + SVG_EXTENSION);
  }

  public Path pngOutputPath() {
    String svgPath = outputPath().toString();
    return Path.of(svgPath.substring(0, svgPath.length() - SVG_EXTENSION.length()) + PNG_EXTENSION);
  }

  private static DiagramTheme expectTheme(String name) {
    DiagramTheme theme = DiagramThemes.find(name);

    if (theme == null) {
      throw new IllegalArgumentException("unknown theme '" + name + "', expected one of: "
              + String.join(", ", DiagramThemes.getNames()));
    }

    return theme;
  }

  private static String valueAfter(String[] args, int index) {
    if (index + 1 >= args.length) {
      throw new IllegalArgumentException("missing value after " + args[index]);
    }

    return args[index + 1];
  }
}
