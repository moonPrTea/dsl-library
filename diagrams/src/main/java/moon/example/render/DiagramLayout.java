package moon.example.render;

import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class DiagramLayout {
  private static final double MARGIN = 80;
  private static final double HEADING_HEIGHT = 92;
  private static final int MIN_WIDTH = 800;
  private static final int MIN_HEIGHT = 480;

  private final int width;
  private final int height;
  private final Map<DiagramNode, NodeBox> boxes;

  private DiagramLayout(int width, int height, Map<DiagramNode, NodeBox> boxes) {
    this.width = width;
    this.height = height;
    this.boxes = boxes;
  }

  public static DiagramLayout arrange(Diagram diagram, DiagramTheme theme,
                                      Function<DiagramNode, NodeSize> sizer) {
    Map<DiagramNode, Integer> levels = assignLevels(diagram);
    Map<Integer, List<DiagramNode>> nodesByLevel = new LinkedHashMap<>();

    for (DiagramNode node : diagram.getNodes()) {
      nodesByLevel.computeIfAbsent(levels.get(node), level -> new ArrayList<>()).add(node);
    }

    double columnGap = theme.metrics().columnGap();
    double rowGap = theme.metrics().rowGap();

    Map<DiagramNode, NodeSize> sizes = new LinkedHashMap<>();
    for (DiagramNode node : diagram.getNodes()) {
      sizes.put(node, sizer.apply(node));
    }

    int width = (int) Math.ceil(Math.max(MIN_WIDTH, MARGIN * 2
            + nodesByLevel.values().stream().mapToDouble(row -> rowWidth(row, sizes, columnGap))
                    .max().orElse(0)));

    Map<DiagramNode, NodeBox> boxes = new LinkedHashMap<>();
    double y = HEADING_HEIGHT;

    for (Map.Entry<Integer, List<DiagramNode>> entry : nodesByLevel.entrySet()) {
      List<DiagramNode> nodes = entry.getValue();
      double x = (width - rowWidth(nodes, sizes, columnGap)) / 2;
      double rowHeight = 0;

      for (DiagramNode node : nodes) {
        NodeSize size = sizes.get(node);
        boxes.put(node, new NodeBox(x, y, size.width(), size.height(), entry.getKey()));
        x += size.width() + columnGap;
        rowHeight = Math.max(rowHeight, size.height());
      }

      y += rowHeight + rowGap;
    }

    int height = (int) Math.ceil(Math.max(MIN_HEIGHT, y - rowGap + MARGIN));
    return new DiagramLayout(width, height, boxes);
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public NodeBox boxOf(DiagramNode node) {
    return boxes.get(node);
  }

  public Map<DiagramNode, NodeBox> getBoxes() {
    return Collections.unmodifiableMap(boxes);
  }

  private static double rowWidth(List<DiagramNode> row, Map<DiagramNode, NodeSize> sizes,
                                 double columnGap) {
    double extent = row.stream().mapToDouble(node -> sizes.get(node).width()).sum();
    return extent + Math.max(0, row.size() - 1) * columnGap;
  }

  private static Map<DiagramNode, Integer> assignLevels(Diagram diagram) {
    Map<DiagramNode, Integer> levels = new LinkedHashMap<>();
    Set<DiagramNode> roots = new LinkedHashSet<>();

    for (DiagramNode node : diagram.getNodes()) {
      if (diagram.getIncomingEdges(node).isEmpty()) {
        roots.add(node);
      }
    }

    if (roots.isEmpty() && !diagram.getNodes().isEmpty()) {
      roots.add(diagram.getNodes().get(0));
    }

    ArrayDeque<DiagramNode> queue = new ArrayDeque<>();

    for (DiagramNode root : roots) {
      levels.put(root, 0);
      queue.add(root);
    }

    while (!queue.isEmpty()) {
      DiagramNode source = queue.removeFirst();
      int nextLevel = levels.get(source) + 1;

      for (DiagramEdge edge : diagram.getOutgoingEdges(source)) {
        if (!levels.containsKey(edge.getTarget())) {
          levels.put(edge.getTarget(), nextLevel);
          queue.addLast(edge.getTarget());
        }
      }
    }

    int nextLevel = levels.values().stream().max(Integer::compareTo).orElse(-1) + 1;

    for (DiagramNode node : diagram.getNodes()) {
      if (!levels.containsKey(node)) {
        levels.put(node, nextLevel++);
      }
    }

    return levels;
  }
}
