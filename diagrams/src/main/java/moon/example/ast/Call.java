package moon.example.ast;

import moon.example.common.TextRange;

import java.util.List;

public record Call(
     String receiverName,
     TextRange receiverNameRange,
     String methodName,
     TextRange methodNameRange,
     List<Argument> arguments,
     TextRange textRange
) implements Statement {
}
