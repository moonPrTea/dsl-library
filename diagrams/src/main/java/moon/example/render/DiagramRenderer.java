package moon.example.render;

import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;

public final class DiagramRenderer {
  public RenderScene render(Diagram diagram, DiagramTheme theme) {
    return render(diagram, theme, null);
  }

  public RenderScene render(Diagram diagram, DiagramTheme theme, String selectedNodeName) {
    DiagramPainter painter = theme.nodeStyle() == NodeStyle.WORKFLOW
            ? new WorkflowPainter(diagram, theme)
            : new FlowchartPainter(theme);
    DiagramLayout layout = DiagramLayout.arrange(diagram, theme, painter::sizeOf);
    RenderScene scene = new RenderScene(layout.getWidth(), layout.getHeight(), theme);

    paintGrid(scene, theme);
    painter.paintHeading(scene, diagram.getTitle(), diagram.getDescription());

    for (DiagramEdge edge : diagram.getEdges()) {
      painter.paintEdge(scene, edge, layout.boxOf(edge.getSource()), layout.boxOf(edge.getTarget()));
    }

    for (DiagramNode node : diagram.getNodes()) {
      boolean selected = node.getName().equals(selectedNodeName);
      painter.paintNode(scene, node, layout.boxOf(node), selected);
    }

    return scene;
  }

  private void paintGrid(RenderScene scene, DiagramTheme theme) {
    double spacing = theme.metrics().gridSpacing();
    String color = theme.colors().grid();

    switch (theme.metrics().gridStyle()) {
      case DOTS -> scene.add(new DotGrid(spacing, 1.1, color));
      case LINES -> scene.add(new LineGrid(spacing, color));
      case NONE -> { }
    }
  }
}
