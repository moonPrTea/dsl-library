package moon.example.render;

import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;
import moon.example.specification.Shape;

import java.util.List;

public final class FlowchartPainter implements DiagramPainter {
  private static final double TITLE_FONT = 20;
  private static final double DESCRIPTION_FONT = 12;
  private static final double NODE_FONT = 14;
  private static final double LABEL_FONT = 11;
  private static final double ARROW_LENGTH = 9;
  private static final double ARROW_HALF_WIDTH = 4.5;
  private static final double BACK_EDGE_CHANNEL_GAP = 14;
  private static final double SELECTED_STROKE_WIDTH = 2.3;

  private final ThemeColors colors;
  private final ThemeMetrics metrics;
  private int backEdgeCount = 0;

  public FlowchartPainter(DiagramTheme theme) {
    this.colors = theme.colors();
    this.metrics = theme.metrics();
  }

  @Override
  public NodeSize sizeOf(DiagramNode node) {
    return new NodeSize(metrics.nodeWidth(), metrics.nodeHeight());
  }

  @Override
  public void paintHeading(RenderScene scene, String title, String description) {
    double centerX = scene.getWidth() / 2.0;

    if (!title.isBlank()) {
      scene.add(new Text(centerX, 35, title, TITLE_FONT, colors.text(), TextAnchor.MIDDLE, true));
    }

    if (!description.isBlank()) {
      scene.add(new Text(centerX, 59, description, DESCRIPTION_FONT, colors.mutedText(),
              TextAnchor.MIDDLE, false));
    }
  }

  @Override
  public void paintNode(RenderScene scene, DiagramNode node, NodeBox box, boolean selected) {
    String stroke = selected ? colors.selection() : colors.nodeBorder();
    double strokeWidth = selected ? SELECTED_STROKE_WIDTH : metrics.nodeStrokeWidth();
    Shape shape = node.getNodeType().shape();

    if (shape == Shape.DIAMOND) {
      scene.add(new Polygon(List.of(box.top(), new Point(box.right(), box.centerY()),
              box.bottomCenter(), box.leftCenter()), colors.nodeFill(), stroke, strokeWidth));
    } else {
      double radius = shape == Shape.ROUNDED_RECTANGLE ? metrics.cornerRadius() : 0;
      scene.add(new Rect(box.x(), box.y(), box.width(), box.height(), radius,
              colors.nodeFill(), stroke, strokeWidth));
    }

    paintCenteredText(scene, DiagramPainter.titleOf(node), box);
  }

  @Override
  public void paintEdge(RenderScene scene, DiagramEdge edge, NodeBox source, NodeBox target) {
    List<Point> route = target.level() <= source.level()
            ? backwardRoute(source, target)
            : forwardRoute(source, target);

    scene.add(new Polyline(route, colors.edge(), metrics.edgeStrokeWidth(), false));
    paintArrow(scene, route.get(route.size() - 2), route.get(route.size() - 1));
    paintLabel(scene, DiagramPainter.labelOf(edge), longestSegmentMiddle(route));
  }

  private List<Point> forwardRoute(NodeBox source, NodeBox target) {
    if (source.centerX() == target.centerX()) {
      return List.of(source.bottomCenter(), target.top());
    }

    double middleY = (source.bottom() + target.y()) / 2;

    return List.of(source.bottomCenter(),
            new Point(source.centerX(), middleY),
            new Point(target.centerX(), middleY),
            target.top());
  }

  private List<Point> backwardRoute(NodeBox source, NodeBox target) {
    double channelX = Math.min(source.x(), target.x()) - 25 - backEdgeCount++ * BACK_EDGE_CHANNEL_GAP;

    return List.of(source.leftCenter(),
            new Point(channelX, source.centerY()),
            new Point(channelX, target.centerY()),
            target.leftCenter());
  }

  private void paintArrow(RenderScene scene, Point before, Point tip) {
    double length = Math.hypot(tip.x() - before.x(), tip.y() - before.y());

    if (length == 0) {
      return;
    }

    double directionX = (tip.x() - before.x()) / length;
    double directionY = (tip.y() - before.y()) / length;
    double baseX = tip.x() - directionX * ARROW_LENGTH;
    double baseY = tip.y() - directionY * ARROW_LENGTH;

    scene.add(new Polygon(List.of(tip,
            new Point(baseX - directionY * ARROW_HALF_WIDTH, baseY + directionX * ARROW_HALF_WIDTH),
            new Point(baseX + directionY * ARROW_HALF_WIDTH, baseY - directionX * ARROW_HALF_WIDTH)),
            colors.edge(), colors.edge(), 1));
  }

  private void paintLabel(RenderScene scene, String label, Point at) {
    double width = TextLayout.estimateWidth(label, LABEL_FONT) + 8;

    scene.add(new Rect(at.x() - width / 2, at.y() - 11, width, 18, 0, colors.canvas(), "none", 0));
    scene.add(new Text(at.x(), at.y() + 3, label, LABEL_FONT, colors.text(), TextAnchor.MIDDLE, false));
  }

  private void paintCenteredText(RenderScene scene, String value, NodeBox box) {
    List<String> lines = TextLayout.wrap(value, box.width() - 36, NODE_FONT, 3);
    double lineHeight = NODE_FONT * 1.25;
    double y = box.centerY() - (lines.size() - 1) * lineHeight / 2 + NODE_FONT * 0.34;

    for (String line : lines) {
      scene.add(new Text(box.centerX(), y, line, NODE_FONT, colors.text(), TextAnchor.MIDDLE, false));
      y += lineHeight;
    }
  }

  private static Point longestSegmentMiddle(List<Point> route) {
    double longest = -1;
    Point middle = route.get(0);

    for (int i = 1; i < route.size(); i++) {
      Point from = route.get(i - 1);
      Point to = route.get(i);
      double length = Math.hypot(to.x() - from.x(), to.y() - from.y());

      if (length > longest) {
        longest = length;
        middle = new Point((from.x() + to.x()) / 2, (from.y() + to.y()) / 2);
      }
    }

    return middle;
  }
}
