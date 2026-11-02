package moon.example.render.element;

public record Rect(
        double x,
        double y,
        double width,
        double height,
        double radius,
        String fill,
        String stroke,
        double strokeWidth
) implements Element {
}
