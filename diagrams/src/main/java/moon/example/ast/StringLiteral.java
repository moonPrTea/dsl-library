package moon.example.ast;

import moon.example.common.TextRange;

public record StringLiteral(
        String value,
        TextRange textRange
) implements Argument {
}
