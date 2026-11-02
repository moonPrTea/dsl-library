package moon.example.render.node;

import moon.example.render.LucideIcon;

import java.util.Locale;

public enum NodeCategory {
  LAUNCH(LucideIcon.ZAP, "#EDE9FE", "#6D5FC4"),
  FINISH(LucideIcon.CIRCLE_X, "#FEE2E2", "#C2483F"),
  ACTION(LucideIcon.COPY, "#DCFCE7", "#4C9A5B"),
  CONDITION(LucideIcon.INFO, "#DBEAFE", "#3B7DD8");

  private final LucideIcon icon;
  private final String background;
  private final String foreground;

  NodeCategory(LucideIcon icon, String background, String foreground) {
    this.icon = icon;
    this.background = background;
    this.foreground = foreground;
  }

  public LucideIcon icon() {
    return icon;
  }

  public String background() {
    return background;
  }

  public String foreground() {
    return foreground;
  }

  public static NodeCategory find(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }

    try {
      return NodeCategory.valueOf(name.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
