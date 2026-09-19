package moon.example.render;

import java.util.List;

public record Polyline(
        List<Point> points,
        String stroke,
        double strokeWidth,
        boolean dashed
) implements Element {
  public Polyline {
    points = List.copyOf(points);
  }
}
