package moon.example.render;

public record DiagramTheme(
        String name,
        NodeStyle nodeStyle,
        ThemeColors colors,
        ThemeMetrics metrics
) {
}
