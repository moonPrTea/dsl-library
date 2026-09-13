package moon.example.ast;

import moon.example.common.TextRange;

public record NumberLiteral(
        String text,
        TextRange textRange
) implements Argument {
}
