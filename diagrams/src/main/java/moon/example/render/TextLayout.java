package moon.example.render;

import java.util.ArrayList;
import java.util.List;

public final class TextLayout {
  private static final double AVERAGE_GLYPH_WIDTH = 0.56;
  private static final String ELLIPSIS = "…";

  private TextLayout() {
  }

  public static double estimateWidth(String value, double fontSize) {
    return value.length() * fontSize * AVERAGE_GLYPH_WIDTH;
  }

  public static String ellipsize(String value, double maxWidth, double fontSize) {
    int limit = charactersPerLine(maxWidth, fontSize);

    return value.length() <= limit
            ? value
            : value.substring(0, Math.max(1, limit - 1)).stripTrailing() + ELLIPSIS;
  }

  public static List<String> wrap(String value, double maxWidth, double fontSize, int maxLines) {
    int limit = charactersPerLine(maxWidth, fontSize);
    List<String> lines = new ArrayList<>();
    String remaining = value.trim();

    while (!remaining.isEmpty() && lines.size() < maxLines) {
      if (remaining.length() <= limit) {
        lines.add(remaining);
        break;
      }

      int split = remaining.lastIndexOf(' ', limit);

      if (split < limit / 2) {
        split = limit;
      }

      String line = remaining.substring(0, split).trim();
      remaining = remaining.substring(split).trim();

      if (lines.size() == maxLines - 1 && !remaining.isEmpty()) {
        line = ellipsize(line + " " + remaining, maxWidth, fontSize);
        remaining = "";
      }

      lines.add(line);
    }

    return lines;
  }

  private static int charactersPerLine(double maxWidth, double fontSize) {
    return Math.max(8, (int) (maxWidth / (fontSize * AVERAGE_GLYPH_WIDTH)));
  }
}
