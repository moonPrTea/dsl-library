package moon.example.lexer;

import moon.example.common.TextRange;

public record Token(
     TokenType tokenType,
     String text,
     TextRange textRange,
     boolean startsNewLine
) {
  public boolean isOfType(TokenType expectedType) {
    return this.tokenType == expectedType;
  }

  @Override
  public String toString() {
    return "%-13s %-18s %s".formatted(tokenType, "\"" + text + "\"", textRange);
  }
}
