package moon.example.ast;

import moon.example.common.TextRange;

public sealed interface Statement permits Declaration, Call {
  TextRange textRange();
}
