package moon.example.specification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class SpecificationRegistryTest {

  @Test
  void testRegistryShouldContainBlockDiagram() {
    SpecificationRegistry registry = new SpecificationRegistry();

    assertTrue(registry.isDiagramType("BlockDiagram"));
    assertEquals("BlockDiagram", registry.find("BlockDiagram").getDiagramTypeName());
  }

  @Test
  void testRegistryShouldReturnNullForUnknownDiagramType() {
    SpecificationRegistry registry = new SpecificationRegistry();

    assertNull(registry.find("SequenceDiagram"));
  }

  @Test
  void testBlockDiagramShouldDefineThreeNodeTypes() {
    DiagramSpecification specification = new SpecificationRegistry().find("BlockDiagram");

    assertEquals(Shape.ROUNDED_RECTANGLE, specification.findNodeType("Terminator").shape());
    assertEquals(Shape.RECTANGLE, specification.findNodeType("Process").shape());
    assertEquals(Shape.DIAMOND, specification.findNodeType("Decision").shape());
  }

  @Test
  void testBlockDiagramShouldResolveRelationSugar() {
    DiagramSpecification specification = new SpecificationRegistry().find("BlockDiagram");

    assertEquals("Next", specification.findRelationSugar("setNext").name());
    assertEquals("Yes", specification.findRelationSugar("setYes").name());
    assertEquals("No", specification.findRelationSugar("setNo").name());
  }

  @Test
  void testBlockDiagramShouldDefineDiagramProperties() {
    DiagramSpecification specification = new SpecificationRegistry().find("BlockDiagram");

    assertEquals(PropertyType.STRING, specification.findDiagramProperty("setTitle"));
    assertEquals(PropertyType.NULLABLE_STRING, specification.findDiagramProperty("setDescription"));
    assertEquals(PropertyType.COLOR, specification.findDiagramProperty("setColor"));
  }

  @Test
  void testNodeTypeShouldReportUnknownProperty() {
    NodeType decision = new SpecificationRegistry().find("BlockDiagram").findNodeType("Decision");

    assertTrue(decision.hasProperty("setTitle"));
    assertNull(decision.getPropertyType("setNxt"));
  }
}
