package moon.example.lexer;

import moon.example.common.FileInformation;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

  @Test
  void testLexerShouldRecognizeIdentifier() {
    Token identifierToken = tokenize("setTitle").get(0);

    assertEquals(TokenType.IDENTIFIER, identifierToken.tokenType());
    assertEquals("setTitle", identifierToken.text());
  }

  @Test
  void testIdentifierShouldAllowDigitsAndUnderscoresAfterFirstCharacter() {
    Token identifierToken = tokenize("_node_2").get(0);

    assertEquals(TokenType.IDENTIFIER, identifierToken.tokenType());
    assertEquals("_node_2", identifierToken.text());
  }

  @Test
  void testLexerShouldRecognizeNewKeyword() {
    assertTokenTypes("new",
            TokenType.NEW,
            TokenType.END_OF_FILE);
  }

  @Test
  void testKeywordShouldNotBeTakenFromLongerIdentifier() {
    assertTokenTypes("new newNode",
            TokenType.NEW,
            TokenType.IDENTIFIER,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldRecognizeNumber() {
    Token numberToken = tokenize("255").get(0);

    assertEquals(TokenType.NUMBER, numberToken.tokenType());
    assertEquals("255", numberToken.text());
  }

  @Test
  void testLexerShouldReportNumberFollowedByLetters() {
    List<Problem> problems = collectProblems("123abc");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.MALFORMED_NUMBER, problems.get(0).code());
  }

  @Test
  void testMalformedNumberShouldNotProduceTokens() {
    assertTokenTypes("123abc ;",
            TokenType.SEMICOLON,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldRecognizeString() {
    Token stringToken = tokenize("\"Login flow\"").get(0);

    assertEquals(TokenType.STRING, stringToken.tokenType());
    assertEquals("Login flow", stringToken.text());
  }

  @Test
  void testStringRangeShouldIncludeQuotes() {
    Token stringToken = tokenize("\"abc\"").get(0);

    assertEquals(0, stringToken.textRange().startIndex());
    assertEquals(5, stringToken.textRange().endIndex());
  }

  @Test
  void testLexerShouldRecognizeEmptyString() {
    Token stringToken = tokenize("\"\"").get(0);

    assertEquals(TokenType.STRING, stringToken.tokenType());
    assertEquals("", stringToken.text());
  }

  @Test
  void testLexerShouldDecodeEscapeSequences() {
    Token stringToken = tokenize("\"a\\\"b\\\\c\\nd\\te\"").get(0);

    assertEquals("a\"b\\c\nd\te", stringToken.text());
  }

  @Test
  void testLexerShouldReportUnknownEscapeSequence() {
    List<Problem> problems = collectProblems("\"bad \\q escape\"");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNKNOWN_ESCAPE, problems.get(0).code());
  }

  @Test
  void testUnknownEscapeShouldNotBreakString() {
    Token stringToken = tokenize("\"bad \\q escape\"").get(0);

    assertEquals(TokenType.STRING, stringToken.tokenType());
    assertEquals("bad q escape", stringToken.text());
  }

  @Test
  void testLexerShouldReportStringUnterminatedAtEndOfLine() {
    List<Problem> problems = collectProblems("\"not closed\n");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNTERMINATED_STRING, problems.get(0).code());
  }

  @Test
  void testLexerShouldReportStringUnterminatedAtEndOfFile() {
    List<Problem> problems = collectProblems("\"not closed");

    assertEquals(1, problems.size());
    assertEquals(ProblemCode.UNTERMINATED_STRING, problems.get(0).code());
  }

  @Test
  void testUnterminatedStringShouldPointAtOpeningQuote() {
    List<Problem> problems = collectProblems("x = \"not closed\ny = 1;");

    assertEquals(4, problems.get(0).range().startIndex());
  }

  @Test
  void testLexerShouldRecoverOnNextLineAfterUnterminatedString() {
    assertTokenTypes("\"not closed\n;",
            TokenType.SEMICOLON,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldTokenizeDeclarationStatement() {
    assertTokenTypes("Terminator start = new Terminator();",
            TokenType.IDENTIFIER,
            TokenType.IDENTIFIER,
            TokenType.EQUALS,
            TokenType.NEW,
            TokenType.IDENTIFIER,
            TokenType.LEFT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.SEMICOLON,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldTokenizeQualifiedCallWithArguments() {
    assertTokenTypes("diagram.setColor(Colors::new(255, 0, 0));",
            TokenType.IDENTIFIER,
            TokenType.DOT,
            TokenType.IDENTIFIER,
            TokenType.LEFT_PAREN,
            TokenType.IDENTIFIER,
            TokenType.DOUBLE_COLON,
            TokenType.NEW,
            TokenType.LEFT_PAREN,
            TokenType.NUMBER,
            TokenType.COMMA,
            TokenType.NUMBER,
            TokenType.COMMA,
            TokenType.NUMBER,
            TokenType.RIGHT_PAREN,
            TokenType.RIGHT_PAREN,
            TokenType.SEMICOLON,
            TokenType.END_OF_FILE);
  }

  @Test
  void testLexerShouldMarkTokensThatStartANewLine() {
    List<Token> tokens = tokenize("a.b()\n.c()");

    assertFalse(tokens.get(0).startsNewLine(), "identifier 'a' opens the file");
    assertFalse(tokens.get(1).startsNewLine(), "'.' right after 'a'");
    assertTrue(tokens.get(5).startsNewLine(), "'.' after the line break");
  }

  @Test
  void testLexerShouldNotMarkTokensOnTheSameLine() {
    List<Token> tokens = tokenize("a . b ( )");

    for (Token token : tokens) {
      if (!token.isOfType(TokenType.END_OF_FILE)) {
        assertFalse(token.startsNewLine(), "tokens: " + tokens);
      }
    }
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
