package moon.example.render.element;

public sealed interface Element
        permits LineGrid, DotGrid, Rect, Circle, Polygon, Polyline, Curve, Text, Icon {
}
