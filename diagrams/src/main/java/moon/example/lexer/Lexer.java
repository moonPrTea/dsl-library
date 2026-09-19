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
  private boolean nextTokenStartsNewLine = false;

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
    allTokens.add(new Token(type, text, new TextRange(startPosition, position),
            nextTokenStartsNewLine));
  }

  public List<Token> tokenizeFile() {
    while (true) {
      nextTokenStartsNewLine = skipSpecialSpacesAndComments();

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

    allTokens.add(new Token(TokenType.END_OF_FILE, "", new TextRange(position, position),
            nextTokenStartsNewLine));

    return allTokens;
  }

  public boolean skipSpecialSpacesAndComments() {
    boolean sawNewLine = false;

    while(!checkAtEnd()) {
      char currentCharacter = peekChar();

      if (currentCharacter == ' ' || currentCharacter == '\t' || currentCharacter == '\n') {
        sawNewLine |= currentCharacter == '\n';
        position++;
      } else if (currentCharacter == '/' && peekAhead(1) == '/') {
        skipLineComment();
      } else if (currentCharacter == '/' && peekAhead(1) == '*') {
        skipBlockComment();
      } else {
        return sawNewLine;
      }
    }

    return sawNewLine;
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

  private void readIdentifier(int startPosition) {
    while (checkIsIdentifierPart(peekChar())) {
      position++;
    }

    String text = content.substring(startPosition, position);
    TokenType tokenType = text.equals("new") ? TokenType.NEW : TokenType.IDENTIFIER;

    addToken(tokenType, text, startPosition);
  }

  private void readNumber(int startPosition) {
    while (checkIsDigit(peekChar())) {
      position++;
    }

    if (checkIsIdentifierStart(peekChar())) {
      while (checkIsIdentifierPart(peekChar())) {
        position++;
      }

      String malformedText = content.substring(startPosition, position);

      problemCollector.add(Problem.error(ProblemCode.MALFORMED_NUMBER, new TextRange(startPosition, position),
              "malformed number literal '" + malformedText + "'",
              "separate the number and the identifier"));
      return;
    }

    addToken(TokenType.NUMBER, content.substring(startPosition, position), startPosition);
  }

  private void readString(int startPosition) {
    StringBuilder value = new StringBuilder();

    while (true) {
      if (checkAtEnd()) {
        reportUnterminatedString(startPosition, "reached end of file");
        return;
      }

      char currentCharacter = peekChar();

      if (currentCharacter == '\n') {
        reportUnterminatedString(startPosition, "reached end of line");
        return;
      }

      if (currentCharacter == '"') {
        position++;
        addToken(TokenType.STRING, value.toString(), startPosition);
        return;
      }

      if (currentCharacter == '\\') {
        readEscapeSequence(value);
        continue;
      }

      value.append(currentCharacter);
      position++;
    }
  }

  private void readEscapeSequence(StringBuilder value) {
    int escapeStartPosition = position;
    position++;

    char escapedCharacter = peekChar();

    switch (escapedCharacter) {
      case '"' -> {
        value.append('"');
        position++;
      }
      case '\\' -> {
        value.append('\\');
        position++;
      }
      case 'n' -> {
        value.append('\n');
        position++;
      }
      case 't' -> {
        value.append('\t');
        position++;
      }
      case '\n', END_OF_TEXT -> { }
      default -> {
        position++;

        problemCollector.add(Problem.error(ProblemCode.UNKNOWN_ESCAPE, new TextRange(escapeStartPosition, position),
                "unknown escape sequence '\\" + escapedCharacter + "'",
                "supported sequences: \\\" \\\\ \\n \\t"));

        value.append(escapedCharacter);
      }
    }
  }

  private void reportUnterminatedString(int startPosition, String detail) {
    problemCollector.add(Problem.error(ProblemCode.UNTERMINATED_STRING, new TextRange(startPosition, startPosition + 1),
            "unterminated string literal: " + detail,
            "add a closing quote"));
  }

  private static boolean checkIsDigit(char character) {
    return character >= '0' && character <= '9';
  }

  private static boolean checkIsIdentifierStart(char character) {
    return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z')
            || character == '_';
  }

  private static boolean checkIsIdentifierPart(char character) {
    return checkIsIdentifierStart(character) || checkIsDigit(character);
  }

}
