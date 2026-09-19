package moon.example.render;

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
