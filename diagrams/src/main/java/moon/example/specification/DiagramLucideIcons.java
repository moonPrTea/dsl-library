package moon.example.specification;

import java.util.List;
import java.util.Locale;

public final class DiagramLucideIcons {
  public record Definition(String name, String description) {
  }

  private static final List<Definition> DEFINITIONS = List.of(
          new Definition("zap", "Lightning bolt for triggers and start actions."),
          new Definition("copy", "Overlapping squares for copy and duplicate actions."),
          new Definition("info", "Information marker for conditions and explanatory steps."),
          new Definition("circle_x", "Circled cross for cancellation, failure, and finish actions."),
          new Definition("question_mark", "Question mark for checks, retries, and unresolved states."));

  private DiagramLucideIcons() {
  }

  public static List<Definition> definitions() {
    return DEFINITIONS;
  }

  public static List<String> names() {
    return DEFINITIONS.stream().map(Definition::name).toList();
  }

  public static Definition find(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }

    String normalized = name.trim().toLowerCase(Locale.ROOT);
    return DEFINITIONS.stream()
            .filter(definition -> definition.name().equals(normalized))
            .findFirst()
            .orElse(null);
  }
}
