package moon.example.ast;

import moon.example.common.TextRange;

import java.util.List;

public record QualifiedName(
     String qualifier,
     String member,
     List<Argument> arguments,
     TextRange textRange
) implements Argument {
}
