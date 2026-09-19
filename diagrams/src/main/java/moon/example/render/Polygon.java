package moon.example.render;

import java.util.List;

public record Polygon(
        List<Point> points,
        String fill,
        String stroke,
        double strokeWidth
) implements Element {
  public Polygon {
    points = List.copyOf(points);
  }
}
