package moon.example.ast;

import moon.example.common.TextRange;

public record VariableReference(
        String name,
        TextRange textRange
) implements Argument {
}
