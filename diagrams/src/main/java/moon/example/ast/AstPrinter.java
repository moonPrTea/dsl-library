package moon.example.ast;

import java.util.List;

public final class AstPrinter {
  private static final String INDENT = "    ";

  private final StringBuilder output = new StringBuilder();

  public static String print(Program program) {
    AstPrinter printer = new AstPrinter();
    printer.printProgram(program);

    return printer.output.toString();
  }

  private void printProgram(Program program) {
    output.append("Program\n");

    for (Statement statement : program.statements()) {
      printStatement(statement, 1);
    }
  }

  private void printStatement(Statement statement, int depth) {
    switch (statement) {
      case Declaration declaration -> printDeclaration(declaration, depth);
      case Call call -> printCall(call, depth);
    }
  }

  private void printDeclaration(Declaration declaration, int depth) {
    appendLine(depth, "Declaration type=" + declaration.typeName()
            + " name=" + declaration.variableName()
            + " constructor=" + declaration.constructorTypeName()
            + " " + declaration.textRange());
  }

  private void printCall(Call call, int depth) {
    appendLine(depth, "Call receiver=" + call.receiverName()
            + " method=" + call.methodName()
            + " " + call.textRange());

    printArguments(call.arguments(), depth + 1);
  }

  private void printArguments(List<Argument> arguments, int depth) {
    for (Argument argument : arguments) {
      printArgument(argument, depth);
    }
  }

  private void printArgument(Argument argument, int depth) {
    switch (argument) {
      case StringLiteral literal ->
              appendLine(depth, "StringLiteral \"" + literal.value() + "\"");
      case NumberLiteral literal ->
              appendLine(depth, "NumberLiteral " + literal.text());
      case VariableReference reference ->
              appendLine(depth, "VariableReference " + reference.name());
      case QualifiedName qualifiedName -> {
        appendLine(depth, "QualifiedName " + qualifiedName.qualifier()
                + "::" + qualifiedName.member());
        printArguments(qualifiedName.arguments(), depth + 1);
      }
    }
  }

  private void appendLine(int depth, String text) {
    output.append(INDENT.repeat(depth)).append(text).append('\n');
  }
}
