package moon.example.render;

import java.util.List;
import java.util.Locale;

public enum LucideIcon {
  ZAP("<path d=\"M13 2 3 14h9l-1 8 10-12h-9z\"/>"),

  COPY("<path d=\"M9 9h11v11H9z\"/>",
          "<path d=\"M14 4H5a1 1 0 0 0-1 1v10\"/>"),
  INFO("<circle cx=\"12\" cy=\"12\" r=\"10\"/>",
          "<path d=\"M12 16v-4\"/>",
          "<path d=\"M12 8h.01\"/>"),
  CIRCLE_X("<circle cx=\"12\" cy=\"12\" r=\"10\"/>",
          "<path d=\"m15 9-6 6\"/>",
          "<path d=\"m9 9 6 6\"/>"),
  QUESTION_MARK("<path d=\"M9 8a3 3 0 0 1 5.5-1.5c.6.8.6 2-.2 2.7L12 11v1.5\"/>",
          "<path d=\"M12 17h.01\"/>");

  private final List<String> elements;

  LucideIcon(String... elements) {
    this.elements = List.of(elements);
  }

  public List<String> elements() {
    return elements;
  }

  public static LucideIcon find(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }

    try {
      return LucideIcon.valueOf(name.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
