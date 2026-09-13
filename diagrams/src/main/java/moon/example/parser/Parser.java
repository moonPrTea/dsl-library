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
import moon.example.common.TextRange;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.lexer.Token;
import moon.example.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class Parser {
  private final List<Token> tokens;
  private final ProblemCollector problemCollector;
  private int position = 0;

  public Parser(List<Token> tokens, ProblemCollector problemCollector) {
    this.tokens = tokens;
    this.problemCollector = problemCollector;
  }

  public Program parseProgram() {
    List<Statement> statements = new ArrayList<>();

    while (!checkAtEnd()) {
      Statement statement = parseStatement();

      if (statement != null) {
        statements.add(statement);
      }
    }

    return new Program(statements);
  }

  private Statement parseStatement() {
    try {
      if (!peekToken().isOfType(TokenType.IDENTIFIER)) {
        throw reportUnexpectedStatementStart();
      }

      if (peekAhead(1).isOfType(TokenType.IDENTIFIER)) {
        return parseDeclaration();
      }

      if (peekAhead(1).isOfType(TokenType.DOT)) {
        return parseCall();
      }

      throw reportUnexpectedStatementStart();
    } catch (ParseException exception) {
      skipToNextStatement();

      return null;
    }
  }

  private Declaration parseDeclaration() {
    Token typeName = expect(TokenType.IDENTIFIER, "type name");
    Token variableName = expect(TokenType.IDENTIFIER, "variable name");
    expect(TokenType.EQUALS, "'='");
    expect(TokenType.NEW, "'new'");
    Token constructorTypeName = expect(TokenType.IDENTIFIER, "type name");
    expect(TokenType.LEFT_PAREN, "'('");
    expect(TokenType.RIGHT_PAREN, "')'");
    Token semicolon = expect(TokenType.SEMICOLON, "';'");

    return new Declaration(
            typeName.text(), typeName.textRange(),
            variableName.text(), variableName.textRange(),
            constructorTypeName.text(), constructorTypeName.textRange(),
            typeName.textRange().unionOf(semicolon.textRange()));
  }

  private Call parseCall() {
    Token receiverName = expect(TokenType.IDENTIFIER, "receiver name");
    expect(TokenType.DOT, "'.'");
    Token methodName = expect(TokenType.IDENTIFIER, "method name");
    expect(TokenType.LEFT_PAREN, "'('");
    List<Argument> arguments = parseArgumentsUntilRightParen();
    expect(TokenType.RIGHT_PAREN, "')'");
    Token semicolon = expect(TokenType.SEMICOLON, "';'");

    return new Call(
            receiverName.text(), receiverName.textRange(),
            methodName.text(), methodName.textRange(),
            arguments,
            receiverName.textRange().unionOf(semicolon.textRange()));
  }

  private List<Argument> parseArgumentsUntilRightParen() {
    List<Argument> arguments = new ArrayList<>();

    if (peekToken().isOfType(TokenType.RIGHT_PAREN)) {
      return arguments;
    }

    arguments.add(parseArgument());

    while (matchesExpected(TokenType.COMMA)) {
      arguments.add(parseArgument());
    }

    return arguments;
  }

  private Argument parseArgument() {
    Token token = peekToken();

    switch (token.tokenType()) {
      case STRING -> {
        advance();

        return new StringLiteral(token.text(), token.textRange());
      }
      case NUMBER -> {
        advance();

        return new NumberLiteral(token.text(), token.textRange());
      }
      case IDENTIFIER -> {
        if (peekAhead(1).isOfType(TokenType.DOUBLE_COLON)) {
          return parseQualifiedName();
        }

        advance();

        return new VariableReference(token.text(), token.textRange());
      }
      default -> throw reportProblem(ProblemCode.UNEXPECTED_ARGUMENT, token,
              "expected an argument but found " + describe(token),
              "an argument is a string, a number, a variable or a qualified name");
    }
  }

  private QualifiedName parseQualifiedName() {
    Token qualifier = expect(TokenType.IDENTIFIER, "qualifier");
    expect(TokenType.DOUBLE_COLON, "'::'");
    Token member = expectMemberName();

    List<Argument> arguments = List.of();
    TextRange endRange = member.textRange();

    if (matchesExpected(TokenType.LEFT_PAREN)) {
      arguments = parseArgumentsUntilRightParen();
      endRange = expect(TokenType.RIGHT_PAREN, "')'").textRange();
    }

    return new QualifiedName(
            qualifier.text(),
            member.text(),
            arguments,
            qualifier.textRange().unionOf(endRange));
  }

  private Token expectMemberName() {
    Token token = peekToken();

    if (token.isOfType(TokenType.IDENTIFIER) || token.isOfType(TokenType.NEW)) {
      return advance();
    }

    throw reportProblem(ProblemCode.EXPECTED_TOKEN, token,
            "expected member name but found " + describe(token));
  }

  private void skipToNextStatement() {
    while (!checkAtEnd()) {
      if (advance().isOfType(TokenType.SEMICOLON)) {
        return;
      }
    }
  }

  private Token expect(TokenType expectedType, String description) {
    Token token = peekToken();

    if (token.isOfType(expectedType)) {
      return advance();
    }

    throw reportProblem(ProblemCode.EXPECTED_TOKEN, token,
            "expected " + description + " but found " + describe(token));
  }

  private ParseException reportUnexpectedStatementStart() {
    Token token = peekToken();

    return reportProblem(ProblemCode.UNEXPECTED_STATEMENT_START, token,
            "expected a statement but found " + describe(token),
            "a statement is a declaration 'Type name = new Type();' or a call 'name.method(...);'");
  }

  private ParseException reportProblem(ProblemCode code, Token token, String message) {
    problemCollector.add(Problem.error(code, token.textRange(), message));

    return new ParseException();
  }

  private ParseException reportProblem(ProblemCode code, Token token, String message, String hint) {
    problemCollector.add(Problem.error(code, token.textRange(), message, hint));

    return new ParseException();
  }

  private static String describe(Token token) {
    return switch (token.tokenType()) {
      case END_OF_FILE -> "end of file";
      case STRING -> "string \"" + token.text() + "\"";
      case NUMBER -> "number " + token.text();
      case IDENTIFIER -> "identifier '" + token.text() + "'";
      default -> "'" + token.text() + "'";
    };
  }

  private boolean checkAtEnd() {
    return peekToken().isOfType(TokenType.END_OF_FILE);
  }

  private Token peekToken() {
    return tokens.get(position);
  }

  private Token peekAhead(int offset) {
    int index = Math.min(position + offset, tokens.size() - 1);

    return tokens.get(index);
  }

  private Token advance() {
    Token token = peekToken();

    if (!checkAtEnd()) {
      position++;
    }

    return token;
  }

  private boolean matchesExpected(TokenType expectedType) {
    if (!peekToken().isOfType(expectedType)) {
      return false;
    }

    position++;

    return true;
  }

  private static final class ParseException extends RuntimeException {
    ParseException() {
      super(null, null, false, false);
    }
  }
}
