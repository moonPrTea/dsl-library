package moon.example.parser;

import moon.example.ast.Argument;
import moon.example.ast.Call;
import moon.example.ast.Declaration;
import moon.example.ast.NumberLiteral;
import moon.example.ast.Program;
import moon.example.ast.QualifiedName;
import moon.example.ast.Statement;
import moon.example.ast.StringLiteral;
import moon.example.ast.VariableReference;
import moon.example.common.FileInformation;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.lexer.Lexer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ParserTest {

  @Test
  void testParserShouldParseEmptyProgram() {
    Program program = parse("");

    assertTrue(program.statements().isEmpty());
  }

  @Test
  void testParserShouldParseDeclaration() {
    Declaration declaration = parseSingle("Terminator start = new Terminator();", Declaration.class);

    assertEquals("Terminator", declaration.typeName());
    assertEquals("start", declaration.variableName());
    assertEquals("Terminator", declaration.constructorTypeName());
  }

  @Test
  void testParserShouldKeepBothTypeNamesWhenTheyDiffer() {
    Declaration declaration = parseSingle("Process p = new Terminator();", Declaration.class);

    assertEquals("Process", declaration.typeName());
    assertEquals("Terminator", declaration.constructorTypeName());
  }

  @Test
  void testDeclarationRangeShouldCoverWholeStatement() {
    Declaration declaration = parseSingle("  Terminator start = new Terminator();", Declaration.class);

    assertEquals(2, declaration.textRange().startIndex());
    assertEquals(38, declaration.textRange().endIndex());
  }

  @Test
  void testParserShouldParseCallWithoutArguments() {
    Call call = parseSingle("diagram.render();", Call.class);

    assertEquals("diagram", call.receiverName());
    assertEquals("render", call.methodName());
    assertTrue(call.arguments().isEmpty());
  }

  @Test
  void testParserShouldParseCallWithStringArgument() {
    Call call = parseSingle("start.setTitle(\"Begin\");", Call.class);

    StringLiteral literal = assertInstanceOf(StringLiteral.class, call.arguments().get(0));
    assertEquals("Begin", literal.value());
  }

  @Test
  void testParserShouldParseCallWithVariableArgument() {
    Call call = parseSingle("start.setNext(read);", Call.class);

    VariableReference reference = assertInstanceOf(VariableReference.class, call.arguments().get(0));
    assertEquals("read", reference.name());
  }

  @Test
  void testParserShouldParseCallWithSeveralArguments() {
    Call call = parseSingle("a.method(\"text\", 42, variable);", Call.class);

    List<Argument> arguments = call.arguments();

    assertEquals(3, arguments.size());
    assertInstanceOf(StringLiteral.class, arguments.get(0));
    assertInstanceOf(NumberLiteral.class, arguments.get(1));
    assertInstanceOf(VariableReference.class, arguments.get(2));
  }

  @Test
  void testParserShouldParseQualifiedNameWithoutArguments() {
    Call call = parseSingle("diagram.setDescription(default::null);", Call.class);

    QualifiedName qualifiedName = assertInstanceOf(QualifiedName.class, call.arguments().get(0));
    assertEquals("default", qualifiedName.qualifier());
    assertEquals("null", qualifiedName.member());
    assertTrue(qualifiedName.arguments().isEmpty());
  }

  @Test
  void testParserShouldParseQualifiedNameWithArguments() {
    Call call = parseSingle("diagram.setColor(Colors::new(255, 0, 128));", Call.class);

    QualifiedName qualifiedName = assertInstanceOf(QualifiedName.class, call.arguments().get(0));
    assertEquals("Colors", qualifiedName.qualifier());
    assertEquals("new", qualifiedName.member());
    assertEquals(3, qualifiedName.arguments().size());
  }

  @Test
  void testParserShouldParseSeveralStatements() {
    Program program = parse("""
            BlockDiagram diagram = new BlockDiagram();
            diagram.setTitle("Login flow");
            Terminator start = new Terminator();
            start.setTitle("Begin");
            """);

    assertEquals(4, program.statements().size());
    assertInstanceOf(Declaration.class, program.statements().get(0));
    assertInstanceOf(Call.class, program.statements().get(1));
    assertInstanceOf(Declaration.class, program.statements().get(2));
    assertInstanceOf(Call.class, program.statements().get(3));
  }

  @Test
  void testParserShouldReportMissingSemicolon() {
    List<Problem> problems = collectProblems("start.setTitle(\"Begin\")");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.EXPECTED_TOKEN, problems.get(0).code());
  }

  @Test
  void testParserShouldReportMissingNewKeyword() {
    List<Problem> problems = collectProblems("Terminator start = Terminator();");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.EXPECTED_TOKEN, problems.get(0).code());
  }

  @Test
  void testParserShouldReportStatementStartingWithNonIdentifier() {
    List<Problem> problems = collectProblems("42;");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNEXPECTED_STATEMENT_START, problems.get(0).code());
  }

  @Test
  void testParserShouldReportInvalidArgument() {
    List<Problem> problems = collectProblems("a.method(=);");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNEXPECTED_ARGUMENT, problems.get(0).code());
  }

  @Test
  void testMissingSemicolonShouldPointAtNextToken() {
    List<Problem> problems = collectProblems("a.b() c.d();");

    assertEquals(6, problems.get(0).range().startIndex());
  }

  @Test
  void testParserShouldRecoverAfterBrokenStatement() {
    Program program = parse("""
            Terminator start = Terminator();
            start.setTitle("Begin");
            """);

    assertEquals(1, program.statements().size());
    assertInstanceOf(Call.class, program.statements().get(0));
  }

  @Test
  void testParserShouldReportEveryBrokenStatementInOneRun() {
    List<Problem> problems = collectProblems("""
            Terminator start = Terminator();
            start.setTitle("Begin");
            read.setNext();
            42;
            check.setYes(=);
            """);

    assertEquals(3, problems.size());
  }

  @Test
  void testParserShouldReportOnlyOneProblemPerBrokenStatement() {
    List<Problem> problems = collectProblems("Terminator = = = ;");

    assertEquals(1, problems.size());
  }

  @Test
  void testParserShouldNotLoopOnMissingSemicolonAtEndOfFile() {
    List<Problem> problems = collectProblems("a.b()");

    assertEquals(1, problems.size());
  }

  private Program parse(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);
    ProblemCollector problemCollector = new ProblemCollector(fileInformation);

    return new Parser(new Lexer(fileInformation, problemCollector).tokenizeFile(), problemCollector)
            .parseProgram();
  }

  private <T extends Statement> T parseSingle(String sourceText, Class<T> expectedClass) {
    Program program = parse(sourceText);

    assertEquals(1, program.statements().size());

    return assertInstanceOf(expectedClass, program.statements().get(0));
  }

  private List<Problem> collectProblems(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);
    ProblemCollector problemCollector = new ProblemCollector(fileInformation);

    new Parser(new Lexer(fileInformation, problemCollector).tokenizeFile(), problemCollector)
            .parseProgram();

    return problemCollector.getAllProblems();
  }
}
