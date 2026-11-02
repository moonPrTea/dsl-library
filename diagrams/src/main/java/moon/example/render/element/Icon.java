package moon.example.render.element;

import moon.example.render.LucideIcon;

public record Icon(
        double x,
        double y,
        double size,
        LucideIcon icon,
        String color
) implements Element {
}
