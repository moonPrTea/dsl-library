package moon.example.analyzer;

import moon.example.common.TextRange;

public record Symbol(
     String name,
     SymbolKind kind,
     String typeName,
     TextRange declarationRange
) {
}
