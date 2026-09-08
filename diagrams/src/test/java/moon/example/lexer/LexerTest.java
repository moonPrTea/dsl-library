package moon.example.lexer;

import moon.example.common.FileInformation;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class LexerTest {
  @Test
  void testLexerShouldRecognizeAllPunctuation() {
    assertTokenTypes(". , ; ( ) = < > ::",
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.SEMICOLON,
            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.EQUALS,
            TokenType.LESS,
            TokenType.GREATER,
            TokenType.DOUBLE_COLON,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldRecognizePunctuationWithoutSpaces() {
    assertTokenTypes(".,;()",
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.SEMICOLON,
            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.END_OF_FILE);
  }

  @Test
  void testTokenRangeShouldCoverExactlyTheLexeme() {
    Token dotToken = tokenize("  .  ").get(0);

    assertEquals(2, dotToken.textRange().startIndex());
    assertEquals(3, dotToken.textRange().endIndex());
    assertEquals(".", dotToken.text());
  }

  @Test
  void testDoubleColonShouldBeOneTokenOfLengthTwo() {
    Token doubleColonToken = tokenize("::").get(0);

    assertEquals(TokenType.DOUBLE_COLON, doubleColonToken.tokenType());
    assertEquals(2, doubleColonToken.textRange().length());
  }

  @Test
  void testEmptyFileShouldProduceOnlyEndOfFileToken() {
    assertTokenTypes("", TokenType.END_OF_FILE);
  }

  @Test
  void testEndOfFileTokenShouldBePlacedAfterLastLexeme() {
    List<Token> tokens = tokenize(".");
    Token lastToken = tokens.get(tokens.size() - 1);

    assertEquals(TokenType.END_OF_FILE, lastToken.tokenType());
    assertEquals(1, lastToken.textRange().startIndex());
    assertTrue(lastToken.textRange().checkEmptiness());
  }

  @Test
  void testLexerShouldSkipWhitespace() {
    assertTokenTypes("  \t \n\n  .  ",
            TokenType.DOT,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldSkipLineComment() {
    assertTokenTypes(". // comment with . and ; inside\n,",
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldSkipBlockComment() {
    assertTokenTypes(". /* several\n lines\n here */ ,",
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldSkipSeveralCommentsInARow() {
    assertTokenTypes("// first\n  /* second */ // third\n .",
            TokenType.DOT,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldHandleCommentAtEndOfFile() {
    assertTokenTypes(". // trailing comment",
            TokenType.DOT,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldReportUnknownCharacter() {
    List<Problem> problems = collectProblems("@");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNKNOWN_CHARACTER, problems.get(0).code());
  }

  @Test
  void testLexerShouldReportLoneColon() {
    List<Problem> problems = collectProblems(":");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.LONE_COLON, problems.get(0).code());
  }

  @Test
  void testLexerShouldReportUnterminatedBlockComment() {
    List<Problem> problems = collectProblems("/* never closed");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNTERMINATED_COMMENT, problems.get(0).code());
  }

  @Test
  void testUnterminatedCommentShouldPointAtOpeningSlash() {
    List<Problem> problems = collectProblems(". /* never closed");

    assertEquals(2, problems.get(0).range().startIndex());
  }

  @Test
  void testLexerShouldReportEveryUnknownCharacterInOneRun() {
    List<Problem> problems = collectProblems("@ . @ , @");

    assertEquals(3, problems.size());
  }

  @Test
  void testLexerShouldProduceTokensAroundUnknownCharacters() {
    assertTokenTypes("@ . @ , @",
            TokenType.DOT,
            TokenType.COMMA,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldReportProblemsInSourceOrder() {
    List<Problem> problems = collectProblems("@ : #");

    assertEquals(3, problems.size());
    assertEquals(ProblemCode.UNKNOWN_CHARACTER, problems.get(0).code());
    assertEquals(ProblemCode.LONE_COLON, problems.get(1).code());
    assertEquals(ProblemCode.UNKNOWN_CHARACTER, problems.get(2).code());
  }

  private List<Token> tokenize(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);

    return new Lexer(fileInformation, new ProblemCollector(fileInformation)).tokenizeFile();
  }

  private List<Problem> collectProblems(String sourceText) {
    FileInformation fileInformation = new FileInformation("test.dgm", sourceText);
    ProblemCollector problemCollector = new ProblemCollector(fileInformation);

    new Lexer(fileInformation, problemCollector).tokenizeFile();

    return problemCollector.getAllProblems();
  }

  private void assertTokenTypes(String sourceText, TokenType... expectedTypes) {
    List<Token> tokens = tokenize(sourceText);

    assertEquals(expectedTypes.length, tokens.size(), "tokens: " + tokens);

    for (int i = 0; i < expectedTypes.length; i++) {
      assertEquals(expectedTypes[i], tokens.get(i).tokenType(), "tokens: " + tokens);
    }
  }
}
