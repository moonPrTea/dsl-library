package moon.example.ast;

import moon.example.common.TextRange;

public sealed interface Argument permits StringLiteral, NumberLiteral, VariableReference, QualifiedName {
  TextRange textRange();
}
