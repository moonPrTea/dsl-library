package moon.example.render.diagram;

import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;
import moon.example.render.node.NodeSize;
import moon.example.render.RenderScene;
import moon.example.render.element.NodeBox;

public interface DiagramPainter {
  NodeSize sizeOf(DiagramNode node);

  void paintHeading(RenderScene scene, String title, String description);

  void paintNode(RenderScene scene, DiagramNode node, NodeBox box, boolean selected);

  void paintEdge(RenderScene scene, DiagramEdge edge, NodeBox source, NodeBox target);

  static String labelOf(DiagramEdge edge) {
    return edge.getLabel().isBlank() ? edge.getRelationType().name() : edge.getLabel();
  }

  static String titleOf(DiagramNode node) {
    return node.getTitle().isBlank() ? node.getNodeType().name() : node.getTitle();
  }
}
