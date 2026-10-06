package moon.example.model;

import moon.example.common.TextRange;
import moon.example.specification.NodeType;

public final class DiagramNode {
  private final String name;
  private final NodeType nodeType;
  private final TextRange declarationRange;
  private String title = "";
  private String badgeLabel = "";
  private String badgeIcon = "";
  private String badgeCategory = "";

  public DiagramNode(String name, NodeType nodeType, TextRange declarationRange) {
    this.name = name;
    this.nodeType = nodeType;
    this.declarationRange = declarationRange;
  }

  public String getName() {
    return name;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public TextRange getDeclarationRange() {
    return declarationRange;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBadgeLabel() {
    return badgeLabel;
  }

  public void setBadgeLabel(String badgeLabel) {
    this.badgeLabel = badgeLabel;
  }

  public String getBadgeIcon() {
    return badgeIcon;
  }

  public void setBadgeIcon(String badgeIcon) {
    this.badgeIcon = badgeIcon;
  }

  public String getBadgeCategory() {
    return badgeCategory;
  }

  public void setBadgeCategory(String badgeCategory) {
    this.badgeCategory = badgeCategory;
  }

  @Override
  public String toString() {
    return name + " : " + nodeType.name() + " \"" + title + "\"";
  }
}
