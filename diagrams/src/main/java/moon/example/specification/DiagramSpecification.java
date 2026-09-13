package moon.example.specification;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class DiagramSpecification {
  private final String diagramTypeName;
  private final Map<String, NodeType> nodeTypes = new LinkedHashMap<>();
  private final Map<String, RelationType> relationTypes = new LinkedHashMap<>();
  private final Map<String, RelationType> relationSugar = new LinkedHashMap<>();
  private final Map<String, PropertyType> diagramProperties = new LinkedHashMap<>();

  protected DiagramSpecification(String diagramTypeName) {
    this.diagramTypeName = diagramTypeName;
  }

  protected void registerNodeType(NodeType nodeType) {
    nodeTypes.put(nodeType.name(), nodeType);
  }

  protected void registerRelationType(RelationType relationType) {
    relationTypes.put(relationType.name(), relationType);
  }

  protected void registerRelationSugar(String methodName, String relationTypeName) {
    RelationType relationType = relationTypes.get(relationTypeName);

    if (relationType == null) {
      throw new IllegalStateException("relation type '" + relationTypeName
              + "' must be registered before sugar method '" + methodName + "'");
    }

    relationSugar.put(methodName, relationType);
  }

  protected void registerDiagramProperty(String methodName, PropertyType propertyType) {
    diagramProperties.put(methodName, propertyType);
  }

  public String getDiagramTypeName() {
    return diagramTypeName;
  }

  public NodeType findNodeType(String name) {
    return nodeTypes.get(name);
  }

  public RelationType findRelationType(String name) {
    return relationTypes.get(name);
  }

  public RelationType findRelationSugar(String methodName) {
    return relationSugar.get(methodName);
  }

  public PropertyType findDiagramProperty(String methodName) {
    return diagramProperties.get(methodName);
  }

  public Collection<NodeType> getNodeTypes() {
    return nodeTypes.values();
  }

  public Collection<RelationType> getRelationTypes() {
    return relationTypes.values();
  }

  public Collection<String> getRelationSugarMethodNames() {
    return relationSugar.keySet();
  }

  public Collection<String> getDiagramMethodNames() {
    return diagramProperties.keySet();
  }
}
