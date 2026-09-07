package moon.example.lexer;

import moon.example.common.FileInformation;
import moon.example.common.TextRange;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;

import java.util.ArrayList;
import java.util.List;

public final class Lexer {
  private static final char END_OF_TEXT = '\0';

  private final String content;
  private final ProblemCollector problemCollector;

  private final List<Token> allTokens = new ArrayList<>();
  private int position = 0;

  public Lexer(FileInformation fileInformation, ProblemCollector problemCollector) {
    this.content = fileInformation.getFileContent();
    this.problemCollector = problemCollector;
  }

  private boolean checkAtEnd() {
    return position >= content.length();
  }

  private char peekChar() {
    return checkAtEnd() ? END_OF_TEXT : content.charAt(position);
  }

  private char peekAhead(int offset) {
    int index = position + offset;

    return (index >= content.length()) ? END_OF_TEXT : content.charAt(index);
  }

  private char advance() {
    return content.charAt(position++);
  }

  private boolean matchesExpected(char expectedChar) {
    if (peekChar() != expectedChar) {
      return false;
    }

    position++;

    return true;
  }

  private void addToken(TokenType type, String text, int startPosition) {
    allTokens.add(new Token(type, text, new TextRange(startPosition, position)));
  }

  public List<Token> tokenizeFile() {
    while (true) {
      skipSpecialSpacesAndComments();

      if (checkAtEnd()) {
        break;
      }

      int startPosition = position;
      char currentCharacter = advance();
      switch (currentCharacter) {
        case '.' -> addToken(TokenType.DOT, ".", startPosition);
        case ',' -> addToken(TokenType.COMMA, ",", startPosition);
        case ';' -> addToken(TokenType.SEMICOLON, ";", startPosition);
        case '(' -> addToken(TokenType.LEFT_PAREN, "(", startPosition);
        case ')' -> addToken(TokenType.RIGHT_PAREN, ")", startPosition);
        case '=' -> addToken(TokenType.EQUALS, "=", startPosition);
        case '<' -> addToken(TokenType.LESS, "<", startPosition);
        case '>' -> addToken(TokenType.GREATER, ">", startPosition);
        case ':' -> read(startPosition);
        case '"' -> readString(startPosition);
        default -> {
          if (checkIsDigit(currentCharacter)) {
            readNumber(startPosition);
          } else if (checkIsIdentifierStart(currentCharacter)) {
            readIdentifier(startPosition);
          } else {
            reportUnknownCharacter(currentCharacter, startPosition);
          }
        }
      }
    }

    allTokens.add(new Token(TokenType.END_OF_FILE, "", new TextRange(position, position)));

    return allTokens;
  }

  public void skipSpecialSpacesAndComments() {
    while(!checkAtEnd()) {
      char currentCharacter = peekChar();

      if (currentCharacter == ' ' || currentCharacter == '\t' || currentCharacter == '\n') {
        position++;
      } else if (currentCharacter == '/' && peekAhead(1) == '/') {
        skipLineComment();
      } else if (currentCharacter == '/' && peekAhead(1) == '*') {
        skipBlockComment();
      } else {
        return;
      }
    }
  }

  private void skipLineComment() {
    while (!checkAtEnd() && peekChar() != '\n') {
      position++;
    }
  }

  private void skipBlockComment() {
    int startPosition = position;
    position += 2;

    while (!checkAtEnd()) {
      if (peekChar() == '*' && peekAhead(1) == '/') {
        position += 2;
        return;
      }
      position++;
    }

    problemCollector.add(Problem.error(ProblemCode.UNTERMINATED_COMMENT, new TextRange(startPosition, startPosition + 2),
            "unterminated block comment",
            "add '*/' before the end of file"));
  }

  private void read(int startPosition) {
    if (matchesExpected(':')) {
      addToken(TokenType.DOUBLE_COLON, "::", startPosition);
      return;
    }

    problemCollector.add(Problem.error(ProblemCode.LONE_COLON, new TextRange(startPosition, position),
            "single colon is not allowed",
            "use '::' for qualified names, for example Colors::new"));
  }

  private void reportUnknownCharacter(char character, int startPosition) {
    String shown = (character < 32)
            ? "\\u%04X".formatted((int) character)
            : String.valueOf(character);

    problemCollector.add(Problem.error(ProblemCode.UNKNOWN_CHARACTER, new TextRange(startPosition, position),
            "unknown character '" + shown + "'"));
  }

  private void readIdentifier(int startPosition) { }
  private void readNumber(int startPosition) { }
  private void readString(int startPosition) { }

  private static boolean checkIsDigit(char character) {
    return character >= '0' && character <= '9';
  }

  private static boolean checkIsIdentifierStart(char character) {
    return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
            || character == '_';
  }

}
