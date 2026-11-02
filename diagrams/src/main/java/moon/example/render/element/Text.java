package moon.example.render.element;

import moon.example.render.text.TextAnchor;

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
