package moon.example.render;

public record DotGrid(
        double spacing,
        double radius,
        String color
) implements Element {
}
