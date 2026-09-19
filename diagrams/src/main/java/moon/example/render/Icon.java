package moon.example.render;

public record Icon(
        double x,
        double y,
        double size,
        LucideIcon icon,
        String color
) implements Element {
}
