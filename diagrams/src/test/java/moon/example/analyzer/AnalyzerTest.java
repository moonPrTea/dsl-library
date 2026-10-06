package moon.example.analyzer;

import moon.example.common.FileInformation;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.diagnostic.ProblemSeverity;
import moon.example.lexer.Lexer;
import moon.example.model.Color;
import moon.example.model.Diagram;
import moon.example.model.DiagramNode;
import moon.example.parser.Parser;
import moon.example.specification.SpecificationRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class AnalyzerTest {
  private static final String DIAGRAM = "BlockDiagram diagram = new BlockDiagram();\n";
  private static final String VALID_FLOW = DIAGRAM + """
          Terminator start = new Terminator();
          Process read = new Process();
          Decision check = new Decision();
          Terminator end = new Terminator();
          start.setNext(read);
          read.setNext(check);
          check.setYes(end);
          check.setNo(read);
          """;

  @Test
  void testAnalyzerShouldBuildModelFromValidProgram() {
    Diagram diagram = analyze(VALID_FLOW);

    assertEquals(4, diagram.getNodes().size());
    assertEquals(4, diagram.getEdges().size());
    assertTrue(collectProblems(VALID_FLOW).isEmpty());
  }

  @Test
  void testAnalyzerShouldApplyDiagramProperties() {
    Diagram diagram = analyze(DIAGRAM + """
            diagram.setTitle("Login flow");
            diagram.setDescription("Checks credentials");
            diagram.setColor(Colors::new(10, 20, 30));
            """);

    assertEquals("Login flow", diagram.getTitle());
    assertEquals("Checks credentials", diagram.getDescription());
    assertEquals(new Color(10, 20, 30), diagram.getBackgroundColor());
  }

  @Test
  void testAnalyzerShouldAcceptDefaultNullForNullableProperty() {
    Diagram diagram = analyze(DIAGRAM + "diagram.setDescription(default::null);\n");

    assertEquals("", diagram.getDescription());
    assertTrue(collectProblems(DIAGRAM + "diagram.setDescription(default::null);\n").isEmpty());
  }

  @Test
  void testAnalyzerShouldApplyNodeTitle() {
    Diagram diagram = analyze(DIAGRAM + """
            Terminator start = new Terminator();
            start.setTitle("Begin");
            diagram.addBlock(start);
            """);

    assertEquals("Begin", diagram.getNodes().get(0).getTitle());
  }

  @Test
  void testAnalyzerShouldApplyNodeBadgeProperties() {
    Diagram diagram = analyze(DIAGRAM + """
            Process start = new Process();
            start.setBadgeLabel("Sync");
            start.setBadgeIcon("zap");
            start.setBadgeCategory("launch");
            diagram.addBlock(start);
            """);

    DiagramNode node = diagram.getNodes().get(0);
    assertEquals("Sync", node.getBadgeLabel());
    assertEquals("zap", node.getBadgeIcon());
    assertEquals("launch", node.getBadgeCategory());
  }

  @Test
  void testAnalyzerShouldCreateEdgeFromSugarMethod() {
    Diagram diagram = analyze(DIAGRAM + """
            Process a = new Process();
            Process b = new Process();
            a.setNext(b);
            """);

    assertEquals(1, diagram.getEdges().size());
    assertEquals("Next", diagram.getEdges().get(0).getRelationType().name());
    assertEquals("a", diagram.getEdges().get(0).getSource().getName());
    assertEquals("b", diagram.getEdges().get(0).getTarget().getName());
  }

  @Test
  void testAnalyzerShouldResolveForwardReferences() {
    List<Problem> problems = collectProblems(DIAGRAM + """
            Process a = new Process();
            a.setNext(b);
            Process b = new Process();
            """);

    assertTrue(problems.isEmpty());
  }

  @Test
  void testAnalyzerShouldReportMissingDiagram() {
    assertSingleProblem("Process a = new Process();", ProblemCode.MISSING_DIAGRAM);
  }

  @Test
  void testAnalyzerShouldReportNodeDeclaredBeforeDiagram() {
    assertSingleProblem("Process a = new Process();\n" + DIAGRAM, ProblemCode.DIAGRAM_NOT_FIRST);
  }

  @Test
  void testAnalyzerShouldReportSecondDiagram() {
    assertSingleProblem(DIAGRAM + "BlockDiagram other = new BlockDiagram();",
            ProblemCode.MULTIPLE_DIAGRAMS);
  }

  @Test
  void testAnalyzerShouldReportDuplicateDeclaration() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            Process a = new Process();
            diagram.addBlock(a);
            """, ProblemCode.DUPLICATE_DECLARATION);
  }

  @Test
  void testAnalyzerShouldReportTypeMismatchInDeclaration() {
    assertSingleProblem(DIAGRAM + "Process a = new Terminator();", ProblemCode.TYPE_MISMATCH);
  }

  @Test
  void testAnalyzerShouldReportUnknownType() {
    assertSingleProblem(DIAGRAM + "Termnator a = new Termnator();", ProblemCode.UNKNOWN_TYPE);
  }

  @Test
  void testUnknownTypeShouldSuggestSimilarName() {
    Problem problem = collectProblems(DIAGRAM + "Termnator a = new Termnator();").get(0);

    assertEquals("did you mean 'Terminator'?", problem.hint());
  }

  @Test
  void testAnalyzerShouldReportUnknownVariable() {
    assertSingleProblem(DIAGRAM + "unknown.setTitle(\"x\");", ProblemCode.UNKNOWN_VARIABLE);
  }

  @Test
  void testAnalyzerShouldReportUnknownVariableInArgument() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            a.setNext(missing);
            """, ProblemCode.UNKNOWN_VARIABLE);
  }

  @Test
  void testAnalyzerShouldReportUnknownNodeMethod() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            a.setNxt(a);
            """, ProblemCode.UNKNOWN_METHOD);
  }

  @Test
  void testUnknownMethodShouldSuggestSimilarName() {
    Problem problem = collectProblems(DIAGRAM + """
            Process a = new Process();
            a.setNxt(a);
            """).get(0);

    assertEquals("did you mean 'setNext'?", problem.hint());
  }

  @Test
  void testUnknownMethodShouldListAvailableMethodsWhenNothingIsSimilar() {
    Problem problem = collectProblems(DIAGRAM + """
            Process a = new Process();
            a.explode(a);
            """).get(0);

    assertEquals("available methods: setTitle, setBadgeLabel, setBadgeIcon, setBadgeCategory, "
            + "setNext, setYes, setNo", problem.hint());
  }

  @Test
  void testAnalyzerShouldReportUnknownDiagramMethod() {
    assertSingleProblem(DIAGRAM + "diagram.setSize(1);", ProblemCode.UNKNOWN_METHOD);
  }

  @Test
  void testAnalyzerShouldReportWrongArgumentCount() {
    assertSingleProblem(DIAGRAM + "diagram.setTitle(\"a\", \"b\");",
            ProblemCode.ARGUMENT_COUNT_MISMATCH);
  }

  @Test
  void testAnalyzerShouldReportNumberWhereStringExpected() {
    assertSingleProblem(DIAGRAM + "diagram.setTitle(42);", ProblemCode.ARGUMENT_TYPE_MISMATCH);
  }

  @Test
  void testAnalyzerShouldReportStringWhereColorExpected() {
    assertSingleProblem(DIAGRAM + "diagram.setColor(\"red\");", ProblemCode.ARGUMENT_TYPE_MISMATCH);
  }

  @Test
  void testAnalyzerShouldReportColorOutOfRange() {
    assertSingleProblem(DIAGRAM + "diagram.setColor(Colors::new(255, 256, 0));",
            ProblemCode.COLOR_OUT_OF_RANGE);
  }

  @Test
  void testAnalyzerShouldReportColorWithWrongComponentCount() {
    assertSingleProblem(DIAGRAM + "diagram.setColor(Colors::new(255, 0));",
            ProblemCode.ARGUMENT_COUNT_MISMATCH);
  }

  @Test
  void testAnalyzerShouldReportNonNodeArgumentInEdge() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            a.setNext("b");
            """, ProblemCode.NOT_A_NODE);
  }

  @Test
  void testAnalyzerShouldReportDiagramUsedAsNode() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            a.setNext(diagram);
            """, ProblemCode.NOT_A_NODE);
  }

  @Test
  void testAnalyzerShouldWarnAboutOrphanNode() {
    List<Problem> problems = collectProblems(DIAGRAM + "Process lonely = new Process();");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.ORPHAN_NODE, problems.get(0).code());
    assertEquals(ProblemSeverity.WARNING, problems.get(0).problemSeverity());
  }

  @Test
  void testOrphanNodeShouldNotEnterModel() {
    Diagram diagram = analyze(DIAGRAM + """
            Process lonely = new Process();
            Process shown = new Process();
            diagram.addBlock(shown);
            """);

    assertEquals(1, diagram.getNodes().size());
    assertEquals("shown", diagram.getNodes().get(0).getName());
  }

  @Test
  void testAnalyzerShouldWarnAboutDuplicateEdge() {
    List<Problem> problems = collectProblems(DIAGRAM + """
            Process a = new Process();
            Process b = new Process();
            a.setNext(b);
            a.setNext(b);
            """);

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.DUPLICATE_EDGE, problems.get(0).code());
  }

  @Test
  void testAnalyzerShouldReportDecisionWithoutNoBranch() {
    assertSingleProblem(DIAGRAM + """
            Decision check = new Decision();
            Process a = new Process();
            check.setYes(a);
            """, ProblemCode.DECISION_MISSING_BRANCH);
  }

  @Test
  void testAnalyzerShouldReportBranchFromProcess() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            Process b = new Process();
            a.setYes(b);
            """, ProblemCode.BRANCH_NOT_FROM_DECISION);
  }

  @Test
  void testAnalyzerShouldReportTerminatorWithTwoOutgoingEdges() {
    assertSingleProblem(DIAGRAM + """
            Terminator start = new Terminator();
            Process a = new Process();
            Process b = new Process();
            start.setNext(a);
            start.setNext(b);
            """, ProblemCode.TERMINATOR_TOO_MANY_OUTGOING);
  }

  @Test
  void testAnalyzerShouldReportCycleWithoutEntry() {
    assertSingleProblem(DIAGRAM + """
            Process a = new Process();
            Process b = new Process();
            a.setNext(b);
            b.setNext(a);
            """, ProblemCode.NO_ENTRY_NODE);
  }

  @Test
  void testAnalyzerShouldSkipStructureRulesWhenThereAreErrors() {
    List<Problem> problems = collectProblems(DIAGRAM + """
            Decision check = new Decision();
            check.setYes(missing);
            """);

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNKNOWN_VARIABLE, problems.get(0).code());
  }

  @Test
  void testAnalyzerShouldReportSeveralProblemsInOneRun() {
    List<Problem> problems = collectProblems(DIAGRAM + """
            Termnator a = new Termnator();
            Process b = new Process();
            b.setNxt(b);
            diagram.setTitle(1);
            """);

    assertEquals(3, problems.size());
  }

  private Diagram analyze(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);
    ProblemCollector problemCollector = new ProblemCollector(fileInformation);
    Diagram diagram = runAnalyzer(fileInformation, problemCollector);

    assertNotNull(diagram, problemCollector.renderProblems());

    return diagram;
  }

  private List<Problem> collectProblems(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);
    ProblemCollector problemCollector = new ProblemCollector(fileInformation);

    runAnalyzer(fileInformation, problemCollector);

    return problemCollector.getAllProblems();
  }

  private void assertSingleProblem(String sourceText, ProblemCode expectedCode) {
    List<Problem> problems = collectProblems(sourceText);

    assertEquals(1, problems.size(), "problems: " + problems);
    assertEquals(expectedCode, problems.get(0).code());
  }

  private Diagram runAnalyzer(FileInformation fileInformation, ProblemCollector problemCollector) {
    return new Analyzer(new SpecificationRegistry(), problemCollector).analyze(
            new Parser(new Lexer(fileInformation, problemCollector).tokenizeFile(), problemCollector)
                    .parseProgram());
  }
}
