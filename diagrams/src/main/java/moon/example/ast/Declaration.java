package moon.example.ast;

import moon.example.common.TextRange;

public record Declaration(
     String typeName,
     TextRange typeNameRange,
     String variableName,
     TextRange variableNameRange,
     String constructorTypeName,
     TextRange constructorTypeNameRange,
     TextRange textRange
) implements Statement {
}
