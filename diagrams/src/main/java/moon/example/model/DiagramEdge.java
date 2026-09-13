package moon.example.model;

import moon.example.common.TextRange;
import moon.example.specification.RelationType;

public final class DiagramEdge {
  private final RelationType relationType;
  private final DiagramNode source;
  private final DiagramNode target;
  private final TextRange declarationRange;
  private String label = "";

  public DiagramEdge(RelationType relationType, DiagramNode source, DiagramNode target,
                     TextRange declarationRange) {
    this.relationType = relationType;
    this.source = source;
    this.target = target;
    this.declarationRange = declarationRange;
  }

  public RelationType getRelationType() {
    return relationType;
  }

  public DiagramNode getSource() {
    return source;
  }

  public DiagramNode getTarget() {
    return target;
  }

  public TextRange getDeclarationRange() {
    return declarationRange;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean connectsSameNodesAs(DiagramEdge other) {
    return source == other.source
            && target == other.target
            && relationType.name().equals(other.relationType.name());
  }

  @Override
  public String toString() {
    return source.getName() + " -> " + target.getName() + " [" + relationType.name() + "]"
            + (label.isEmpty() ? "" : " \"" + label + "\"");
  }
}
