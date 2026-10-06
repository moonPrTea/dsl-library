package moon.example.specification;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class BlockDiagramSpecification extends DiagramSpecification {

  public BlockDiagramSpecification() {
    super("BlockDiagram");

    registerDiagramProperty("setTitle", PropertyType.STRING);
    registerDiagramProperty("setDescription", PropertyType.NULLABLE_STRING);
    registerDiagramProperty("setColor", PropertyType.COLOR);

    Map<String, PropertyType> nodeProperties = new LinkedHashMap<>();
    nodeProperties.put("setTitle", PropertyType.STRING);
    nodeProperties.put("setBadgeLabel", PropertyType.STRING);
    nodeProperties.put("setBadgeIcon", PropertyType.STRING);
    nodeProperties.put("setBadgeCategory", PropertyType.STRING);

    registerNodeType(new NodeType("Terminator", Shape.ROUNDED_RECTANGLE, nodeProperties));
    registerNodeType(new NodeType("Process", Shape.RECTANGLE, nodeProperties));
    registerNodeType(new NodeType("Decision", Shape.DIAMOND, nodeProperties));

    registerRelationType(new RelationType("Next", true, Set.of()));
    registerRelationType(new RelationType("Yes", true, Set.of()));
    registerRelationType(new RelationType("No", true, Set.of()));

    registerRelationSugar("setNext", "Next");
    registerRelationSugar("setYes", "Yes");
    registerRelationSugar("setNo", "No");
  }
}
