package moon.example.render.diagram;

import moon.example.render.node.NodeStyle;
import moon.example.render.theme.ThemeColors;
import moon.example.render.theme.ThemeMetrics;

public record DiagramTheme(
        String name,
        NodeStyle nodeStyle,
        ThemeColors colors,
        ThemeMetrics metrics
) {
}
