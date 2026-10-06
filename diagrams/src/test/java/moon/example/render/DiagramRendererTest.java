package moon.example.render;

import moon.example.common.TextRange;
import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;
import moon.example.specification.BlockDiagramSpecification;
import moon.example.specification.DiagramSpecification;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiagramRendererTest {
  @Test
  void testDefaultThemeShouldDrawFlowchartShapesTopToBottom() {
    Diagram diagram = diagram();
    DiagramRenderer renderer = new DiagramRenderer();
    RenderScene scene = renderer.render(diagram, DiagramThemes.DEFAULT);
    String svg = new SvgDiagramExporter().export(scene);
    DiagramLayout layout = DiagramLayout.arrange(diagram, DiagramThemes.DEFAULT,
            node -> new NodeSize(190, 78));

    assertTrue(svg.contains("data-theme=\"default\""));
    assertTrue(svg.contains("<polygon points="), "decision must be a diamond");
    assertTrue(svg.contains("<polyline points="), "flowchart edges must be orthogonal");
    assertFalse(svg.contains("<pattern"));
    assertTrue(layout.boxOf(diagram.getNodes().get(0)).y()
            < layout.boxOf(diagram.getNodes().get(1)).y());
  }

  @Test
  void testCustomThemeShouldDrawWorkflowCardsWithCategoryPills() {
    Diagram diagram = diagram();
    RenderScene scene = new DiagramRenderer().render(diagram, DiagramThemes.CUSTOM, "check");
    String svg = new SvgDiagramExporter().export(scene);

    assertTrue(svg.contains("data-theme=\"custom\""));
    assertTrue(svg.contains("<circle cx="), "dot grid must be drawn as plain circles");
    assertTrue(svg.contains(" C "), "connectors must curve when they branch");
    assertTrue(svg.contains(">Default</text>"), "category pills use a plain default caption");
    assertTrue(svg.contains(">yes</text>"), "relation names label the branch pills");
    assertTrue(svg.contains(">no</text>"));
    assertTrue(svg.contains("stroke=\"" + DiagramThemes.CUSTOM.colors().selection() + "\""));
    assertFalse(svg.contains("stroke-dasharray"), "connectors must be solid lines, not dashed");
    assertTrue(svg.contains(LucideIcon.ZAP.elements().get(0)),
            "the launch badge must draw the Lucide zap glyph");
    assertTrue(svg.contains("stroke-width=\"2\""), "Lucide icons are stroked, not filled");
    assertTrue(svg.contains("fill=\"" + DiagramThemes.CUSTOM.colors().nodeFill() + "\""),
            "branch labels sit on a plain white box");
  }

  @Test
  void testWorkflowCardsShouldGrowWithLongTitles() {
    Diagram diagram = diagram();
    WorkflowPainter painter = new WorkflowPainter(diagram, DiagramThemes.CUSTOM);
    DiagramNode start = diagram.getNodes().get(0);
    DiagramNode check = diagram.getNodes().get(2);
    check.setTitle("Are the submitted credentials still valid for this session?");

    assertTrue(painter.sizeOf(check).width() > painter.sizeOf(start).width());
  }

  @Test
  void testExporterShouldEscapeText() {
    Diagram diagram = diagram();
    diagram.setTitle("A < B & C");

    String svg = new SvgDiagramExporter().export(
            new DiagramRenderer().render(diagram, DiagramThemes.DEFAULT));

    assertTrue(svg.contains("A &lt; B &amp; C"));
    assertFalse(svg.contains("A < B"));
  }

  @Test
  void testThemesShouldBeFoundByNameIgnoringCase() {
    assertEquals(DiagramThemes.CUSTOM, DiagramThemes.find("Custom"));
    assertEquals(null, DiagramThemes.find("neon"));
  }

  @Test
  void testNodeCategoriesAndIconsShouldBeFoundByNameIgnoringCase() {
    assertEquals(NodeCategory.CONDITION, NodeCategory.find("condition"));
    assertEquals(null, NodeCategory.find("nonsense"));
    assertEquals(LucideIcon.ZAP, LucideIcon.find("Zap"));
    assertEquals(null, LucideIcon.find("nonsense"));
  }

  @Test
  void testCustomThemeShouldUseExplicitBadgeOverrides() {
    Diagram diagram = diagram();
    DiagramNode check = diagram.getNodes().get(2);
    check.setBadgeLabel("Retry");
    check.setBadgeIcon("question_mark");
    check.setBadgeCategory("action");

    RenderScene scene = new DiagramRenderer().render(diagram, DiagramThemes.CUSTOM, "check");
    String svg = new SvgDiagramExporter().export(scene);

    assertTrue(svg.contains(">Retry</text>"), "an explicit badge label must replace the default");
    assertTrue(svg.contains(LucideIcon.QUESTION_MARK.elements().get(0)),
            "an explicit badge icon must replace the shape-derived one");
    assertTrue(svg.contains(NodeCategory.ACTION.background()),
            "an explicit badge category must replace the shape-derived one");
  }

  private static Diagram diagram() {
    TextRange range = new TextRange(0, 0);
    DiagramSpecification specification = new BlockDiagramSpecification();
    Diagram diagram = new Diagram(specification);
    diagram.setTitle("Login flow");

    DiagramNode start = new DiagramNode("start", specification.findNodeType("Terminator"), range);
    DiagramNode read = new DiagramNode("read", specification.findNodeType("Process"), range);
    DiagramNode check = new DiagramNode("check", specification.findNodeType("Decision"), range);
    DiagramNode end = new DiagramNode("end", specification.findNodeType("Terminator"), range);
    start.setTitle("Begin");
    read.setTitle("Read credentials");
    check.setTitle("Credentials valid?");
    end.setTitle("End");

    diagram.addNode(start);
    diagram.addNode(read);
    diagram.addNode(check);
    diagram.addNode(end);
    diagram.addEdge(new DiagramEdge(specification.findRelationType("Next"), start, read, range));
    diagram.addEdge(new DiagramEdge(specification.findRelationType("Next"), read, check, range));
    diagram.addEdge(new DiagramEdge(specification.findRelationType("Yes"), check, end, range));
    diagram.addEdge(new DiagramEdge(specification.findRelationType("No"), check, read, range));

    return diagram;
  }
}
