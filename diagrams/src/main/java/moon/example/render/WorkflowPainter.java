package moon.example.render;

import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;

import java.util.List;
import java.util.Locale;

public final class WorkflowPainter implements DiagramPainter {
  private static final String DEFAULT_BADGE_LABEL = "Default";

  private static final double PILL_HEIGHT = 22;
  private static final double PILL_CARD_GAP = 4;
  private static final double PILL_PROTRUSION = PILL_HEIGHT + PILL_CARD_GAP;
  private static final double PILL_SIDE_MARGIN = 20;
  private static final double PILL_ICON_SIZE = 12;
  private static final double PILL_ICON_GAP = 6;
  private static final double PILL_HORIZONTAL_PADDING = 10;
  private static final double PILL_FONT_SIZE = 12;
  private static final double BRANCH_RADIUS = 6;

  private static final double CARD_FONT_SIZE = 14;
  private static final double CARD_LINE_HEIGHT = CARD_FONT_SIZE * 1.3;
  private static final double CARD_VERTICAL_PADDING = 8;
  private static final double CARD_HORIZONTAL_PADDING = PILL_HORIZONTAL_PADDING;
  private static final double CARD_MAX_TEXT_WIDTH = 340;

  private static final double TITLE_FONT = 15;
  private static final double DESCRIPTION_FONT = 12;

  private static final double DOT_RADIUS = 3;
  private static final double ARROW_LENGTH = 8;
  private static final double ARROW_WIDTH = 7;
  private static final double ARROW_ROUNDING = 2;
  private static final double SELECTION_STROKE_WIDTH = 2;
  private static final double SELECTION_HALO_OFFSET = 3;
  private static final double BRANCH_DROP = 64;
  private static final double LOOP_CHANNEL_GAP = 40;
  private static final double LOOP_CHANNEL_STEP = 36;

  private static final String BRANCH_FOREGROUND = "#374151";

  private final Diagram diagram;
  private final ThemeColors colors;
  private final ThemeMetrics metrics;
  private int loopEdgeCount = 0;

  public WorkflowPainter(Diagram diagram, DiagramTheme theme) {
    this.diagram = diagram;
    this.colors = theme.colors();
    this.metrics = theme.metrics();
  }

  @Override
  public NodeSize sizeOf(DiagramNode node) {
    List<String> lines = titleLines(node);
    double textWidth = lines.stream()
            .mapToDouble(line -> TextLayout.estimateWidth(line, CARD_FONT_SIZE))
            .max().orElse(0);
    double width = Math.max(metrics.nodeWidth(), textWidth + CARD_HORIZONTAL_PADDING * 2);
    double badgeWidth = categoryBadgeWidth() + PILL_SIDE_MARGIN * 2;
    width = Math.max(width, badgeWidth);
    double cardHeight = lines.size() * CARD_LINE_HEIGHT + CARD_VERTICAL_PADDING * 2;

    return new NodeSize(width, PILL_PROTRUSION + cardHeight);
  }

  @Override
  public void paintHeading(RenderScene scene, String title, String description) {
    if (!title.isBlank()) {
      scene.add(new Text(28, 34, title, TITLE_FONT, colors.text(), TextAnchor.START, true));
    }

    if (!description.isBlank()) {
      scene.add(new Text(28, 54, description, DESCRIPTION_FONT, colors.mutedText(),
              TextAnchor.START, false));
    }
  }

  @Override
  public void paintNode(RenderScene scene, DiagramNode node, NodeBox box, boolean selected) {
    NodeCategory category = categoryOf(node);
    double cardTop = box.y() + PILL_PROTRUSION;
    double cardHeight = box.bottom() - cardTop;
    double radius = metrics.cornerRadius();

    if (selected) {
      scene.add(new Rect(box.x() - SELECTION_HALO_OFFSET, cardTop - SELECTION_HALO_OFFSET,
              box.width() + SELECTION_HALO_OFFSET * 2, cardHeight + SELECTION_HALO_OFFSET * 2,
              radius + SELECTION_HALO_OFFSET, "none", colors.selection(), 1));
    }

    scene.add(new Rect(box.x(), cardTop, box.width(), cardHeight, radius, colors.nodeFill(),
            selected ? colors.selection() : colors.nodeBorder(),
            selected ? SELECTION_STROKE_WIDTH : metrics.nodeStrokeWidth()));

    List<String> lines = titleLines(node);
    double textBlockHeight = lines.size() * CARD_LINE_HEIGHT;
    double firstBaseline = cardTop + (cardHeight - textBlockHeight) / 2
            + CARD_LINE_HEIGHT / 2 + CARD_FONT_SIZE * 0.3;

    for (int i = 0; i < lines.size(); i++) {
      scene.add(new Text(box.centerX(), firstBaseline + i * CARD_LINE_HEIGHT,
              lines.get(i), CARD_FONT_SIZE, colors.text(), TextAnchor.MIDDLE, true));
    }

    paintCategoryBadge(scene, box.centerX(), box.y() + PILL_HEIGHT / 2, category);
  }

  @Override
  public void paintEdge(RenderScene scene, DiagramEdge edge, NodeBox source, NodeBox target) {
    if (target.level() <= source.level()) {
      paintLoopEdge(scene, edge, source, target);
    } else if (diagram.getOutgoingEdges(edge.getSource()).size() > 1) {
      paintBranchEdge(scene, edge, source, target);
    } else {
      paintDirectEdge(scene, source, target);
    }
  }

  private void paintDirectEdge(RenderScene scene, NodeBox source, NodeBox target) {
    Point start = new Point(source.centerX(), source.bottom());
    Point tip = new Point(target.centerX(), target.y());
    Point lineEnd = new Point(tip.x(), tip.y() - ARROW_LENGTH);

    scene.add(new Circle(start.x(), start.y(), DOT_RADIUS, colors.edge(), colors.edge(), 0));

    if (Math.abs(start.x() - lineEnd.x()) < 0.5) {
      scene.add(new Polyline(List.of(start, lineEnd), colors.edge(), metrics.edgeStrokeWidth(),
              false));
    } else {
      double midY = (start.y() + lineEnd.y()) / 2;
      scene.add(new Curve(start, new Point(start.x(), midY), new Point(lineEnd.x(), midY), lineEnd,
              colors.edge(), metrics.edgeStrokeWidth(), false));
    }

    paintArrow(scene, tip, 0, 1);
  }

  private void paintBranchEdge(RenderScene scene, DiagramEdge edge, NodeBox source, NodeBox target) {
    Point start = new Point(source.centerX(), source.bottom());
    double pillCenterY = start.y() + BRANCH_DROP;
    Point pillCenter = new Point(target.centerX(), pillCenterY);
    Point arrowTip = new Point(pillCenter.x(), pillCenterY - PILL_HEIGHT / 2);
    Point forkTip = new Point(arrowTip.x(), arrowTip.y() - ARROW_LENGTH);
    double midY = (start.y() + forkTip.y()) / 2;

    scene.add(new Circle(start.x(), start.y(), DOT_RADIUS, colors.edge(), colors.edge(), 0));
    scene.add(new Curve(start, new Point(start.x(), midY), new Point(forkTip.x(), midY), forkTip,
            colors.edge(), metrics.edgeStrokeWidth(), false));
    paintArrow(scene, arrowTip, 0, 1);
    paintBranchBadge(scene, pillCenter, DiagramPainter.labelOf(edge));

    Point afterPill = new Point(target.centerX(), pillCenterY + PILL_HEIGHT / 2);
    Point tip = new Point(target.centerX(), target.y());
    Point lineEnd = new Point(tip.x(), tip.y() - ARROW_LENGTH);
    scene.add(new Polyline(List.of(afterPill, lineEnd), colors.edge(), metrics.edgeStrokeWidth(),
            false));
    paintArrow(scene, tip, 0, 1);
  }

  private void paintLoopEdge(RenderScene scene, DiagramEdge edge, NodeBox source, NodeBox target) {
    double sourceY = cardCenterY(source);
    double targetY = cardCenterY(target);
    double channelX = Math.max(source.right(), target.right())
            + LOOP_CHANNEL_GAP + loopEdgeCount++ * LOOP_CHANNEL_STEP;
    double pillY = (sourceY + targetY) / 2;
    Point start = new Point(source.right(), sourceY);
    Point tip = new Point(target.right(), targetY);
    Point beforePill = new Point(channelX, pillY + PILL_HEIGHT / 2);
    Point afterPill = new Point(channelX, pillY - PILL_HEIGHT / 2);
    Point arrowBase = new Point(tip.x() + ARROW_LENGTH, tip.y());

    scene.add(new Circle(start.x(), start.y(), DOT_RADIUS, colors.edge(), colors.edge(), 0));
    scene.add(new Curve(start, new Point((start.x() + channelX) / 2, start.y()),
            new Point(channelX, beforePill.y()), beforePill,
            colors.edge(), metrics.edgeStrokeWidth(), false));
    paintBranchBadge(scene, new Point(channelX, pillY), DiagramPainter.labelOf(edge));
    scene.add(new Curve(afterPill, new Point(channelX, arrowBase.y()),
            new Point((channelX + arrowBase.x()) / 2, arrowBase.y()), arrowBase,
            colors.edge(), metrics.edgeStrokeWidth(), false));
    paintArrow(scene, tip, -1, 0);
  }

  private double cardCenterY(NodeBox box) {
    double cardTop = box.y() + PILL_PROTRUSION;
    return (cardTop + box.bottom()) / 2;
  }

  private void paintArrow(RenderScene scene, Point tip, double directionX, double directionY) {
    double normalX = -directionY;
    double normalY = directionX;
    Point vertex = new Point(tip.x() - directionX * ARROW_ROUNDING / 2,
            tip.y() - directionY * ARROW_ROUNDING / 2);
    Point base = new Point(vertex.x() - directionX * ARROW_LENGTH,
            vertex.y() - directionY * ARROW_LENGTH);

    scene.add(new Polygon(List.of(
            new Point(base.x() + normalX * ARROW_WIDTH / 2, base.y() + normalY * ARROW_WIDTH / 2),
            vertex,
            new Point(base.x() - normalX * ARROW_WIDTH / 2, base.y() - normalY * ARROW_WIDTH / 2)),
            colors.edge(), colors.edge(), ARROW_ROUNDING));
  }

  private void paintCategoryBadge(RenderScene scene, double centerX, double centerY,
                                  NodeCategory category) {
    double width = categoryBadgeWidth();
    double left = centerX - width / 2;

    scene.add(new Rect(left, centerY - PILL_HEIGHT / 2, width, PILL_HEIGHT, PILL_HEIGHT / 2,
            category.background(), "none", 0));

    double iconX = left + PILL_HORIZONTAL_PADDING;
    scene.add(new Icon(iconX, centerY - PILL_ICON_SIZE / 2, PILL_ICON_SIZE, category.icon(),
            category.foreground()));

    double textX = iconX + PILL_ICON_SIZE + PILL_ICON_GAP;
    scene.add(new Text(textX, centerY + 4, DEFAULT_BADGE_LABEL, PILL_FONT_SIZE,
            category.foreground(), TextAnchor.START, true));
  }

  private double categoryBadgeWidth() {
    return PILL_ICON_SIZE + PILL_ICON_GAP + TextLayout.estimateWidth(DEFAULT_BADGE_LABEL, PILL_FONT_SIZE)
            + PILL_HORIZONTAL_PADDING * 2;
  }

  private void paintBranchBadge(RenderScene scene, Point center, String relationLabel) {
    String label = relationLabel.toLowerCase(Locale.ROOT);
    double width = branchBadgeWidth(label);
    double left = center.x() - width / 2;

    scene.add(new Rect(left, center.y() - PILL_HEIGHT / 2, width, PILL_HEIGHT, BRANCH_RADIUS,
            colors.nodeFill(), colors.nodeBorder(), metrics.nodeStrokeWidth()));
    scene.add(new Text(center.x(), center.y() + 4, label, PILL_FONT_SIZE, BRANCH_FOREGROUND,
            TextAnchor.MIDDLE, true));
  }

  private double branchBadgeWidth(String label) {
    return TextLayout.estimateWidth(label, PILL_FONT_SIZE) + PILL_HORIZONTAL_PADDING * 2;
  }

  private List<String> titleLines(DiagramNode node) {
    return TextLayout.wrap(DiagramPainter.titleOf(node), CARD_MAX_TEXT_WIDTH, CARD_FONT_SIZE, 2);
  }

  private NodeCategory categoryOf(DiagramNode node) {
    return switch (node.getNodeType().shape()) {
      case DIAMOND -> NodeCategory.CONDITION;
      case RECTANGLE -> NodeCategory.ACTION;
      case ROUNDED_RECTANGLE -> diagram.getIncomingEdges(node).isEmpty()
              ? NodeCategory.LAUNCH
              : NodeCategory.FINISH;
    };
  }
}
