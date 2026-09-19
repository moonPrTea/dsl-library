package moon.example.render;

public record Text(
        double x,
        double y,
        String value,
        double fontSize,
        String color,
        TextAnchor anchor,
        boolean bold
) implements Element {
}
