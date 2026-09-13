package moon.example.model;

public final class ModelPrinter {

  private ModelPrinter() {
  }

  public static String print(Diagram diagram) {
    StringBuilder output = new StringBuilder();

    output.append("Diagram: ").append(diagram.getSpecification().getDiagramTypeName())
            .append(" \"").append(diagram.getTitle()).append("\"")
            .append(" color=").append(diagram.getBackgroundColor().toHex())
            .append('\n');

    output.append("Nodes (").append(diagram.getNodes().size()).append("):\n");
    for (DiagramNode node : diagram.getNodes()) {
      output.append("  ").append(node).append('\n');
    }

    output.append("Edges (").append(diagram.getEdges().size()).append("):\n");
    for (DiagramEdge edge : diagram.getEdges()) {
      output.append("  ").append(edge).append('\n');
    }

    return output.toString();
  }
}
