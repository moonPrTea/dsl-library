package moon.example.specification;

import java.util.Map;
import java.util.Set;

public final class BlockDiagramSpecification extends DiagramSpecification {

  public BlockDiagramSpecification() {
    super("BlockDiagram");

    registerDiagramProperty("setTitle", PropertyType.STRING);
    registerDiagramProperty("setDescription", PropertyType.NULLABLE_STRING);
    registerDiagramProperty("setColor", PropertyType.COLOR);

    registerNodeType(new NodeType("Terminator", Shape.ROUNDED_RECTANGLE,
            Map.of("setTitle", PropertyType.STRING)));

    registerNodeType(new NodeType("Process", Shape.RECTANGLE,
            Map.of("setTitle", PropertyType.STRING)));

    registerNodeType(new NodeType("Decision", Shape.DIAMOND,
            Map.of("setTitle", PropertyType.STRING)));

    registerRelationType(new RelationType("Next", true, Set.of()));
    registerRelationType(new RelationType("Yes", true, Set.of()));
    registerRelationType(new RelationType("No", true, Set.of()));

    registerRelationSugar("setNext", "Next");
    registerRelationSugar("setYes", "Yes");
    registerRelationSugar("setNo", "No");
  }
}
