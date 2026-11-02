package moon.example.render.diagram;

import moon.example.render.GridStyle;
import moon.example.render.node.NodeStyle;
import moon.example.render.theme.ThemeColors;
import moon.example.render.theme.ThemeMetrics;

import java.util.List;
import java.util.Locale;

public final class DiagramThemes {
  public static final DiagramTheme DEFAULT = new DiagramTheme("default", NodeStyle.FLOWCHART,
          new ThemeColors("#FFFFFF", "#FFFFFF", "#F2F2F2", "#333333",
                  "#111111", "#555555", "#111111", "#1F6FD1"),
          new ThemeMetrics(160, 64, 14, 1.3, 1.4, GridStyle.NONE, 0, 72, 90));

  public static final DiagramTheme CUSTOM = new DiagramTheme("custom", NodeStyle.WORKFLOW,
          new ThemeColors("none", "rgba(17, 24, 39, 0.16)", "#FFFFFF", "#E5E7EB",
                  "#111827", "#6B7280", "#9CA3AF", "#4C8BE0"),
          new ThemeMetrics(200, 60, 10, 1, 1.5, GridStyle.DOTS, 18, 56, 150));

  private static final List<DiagramTheme> ALL = List.of(DEFAULT, CUSTOM);

  private DiagramThemes() {
  }

  public static List<String> getNames() {
    return ALL.stream().map(DiagramTheme::name).toList();
  }

  public static DiagramTheme find(String name) {
    String normalized = name.toLowerCase(Locale.ROOT);

    return ALL.stream()
            .filter(theme -> theme.name().equals(normalized))
            .findFirst()
            .orElse(null);
  }
}
