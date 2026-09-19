package moon.example.render;

public record Circle(
        double centerX,
        double centerY,
        double radius,
        String fill,
        String stroke,
        double strokeWidth
) implements Element {
}
