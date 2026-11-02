package moon.example.render.theme;

import moon.example.render.GridStyle;

public record ThemeMetrics(
        double nodeWidth,
        double nodeHeight,
        double cornerRadius,
        double nodeStrokeWidth,
        double edgeStrokeWidth,
        GridStyle gridStyle,
        double gridSpacing,
        double columnGap,
        double rowGap
) {
}
