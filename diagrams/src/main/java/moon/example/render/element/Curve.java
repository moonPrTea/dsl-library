package moon.example.render.element;

public record Curve(
        Point start,
        Point control1,
        Point control2,
        Point end,
        String stroke,
        double strokeWidth,
        boolean dashed
) implements Element {
}
