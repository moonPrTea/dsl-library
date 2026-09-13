package moon.example.model;

import moon.example.specification.DiagramSpecification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Diagram {
  private final DiagramSpecification specification;
  private final List<DiagramNode> nodes = new ArrayList<>();
  private final List<DiagramEdge> edges = new ArrayList<>();
  private String title = "";
  private String description = "";
  private Color backgroundColor = Color.WHITE;

  public Diagram(DiagramSpecification specification) {
    this.specification = specification;
  }

  public DiagramSpecification getSpecification() {
    return specification;
  }

  public List<DiagramNode> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  public List<DiagramEdge> getEdges() {
    return Collections.unmodifiableList(edges);
  }

  public void addNode(DiagramNode node) {
    nodes.add(node);
  }

  public void addEdge(DiagramEdge edge) {
    edges.add(edge);
  }

  public List<DiagramEdge> getOutgoingEdges(DiagramNode node) {
    return edges.stream().filter(edge -> edge.getSource() == node).toList();
  }

  public List<DiagramEdge> getIncomingEdges(DiagramNode node) {
    return edges.stream().filter(edge -> edge.getTarget() == node).toList();
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Color getBackgroundColor() {
    return backgroundColor;
  }

  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;
  }
}
