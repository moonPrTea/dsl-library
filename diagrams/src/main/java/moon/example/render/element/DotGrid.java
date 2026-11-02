package moon.example.render.element;

public record DotGrid(
        double spacing,
        double radius,
        String color
) implements Element {
}
