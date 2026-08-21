package moon.example.diagram;

import moon.example.common.TextRange;

public record ProblemDiagnostic(
        ProblemType problemType,
        ProblemCode code,
        TextRange range,
        String message,
        String hint
) {
  public ProblemDiagnostic {
    if (problemType == null || code == null || range == null || message == null) {
      throw new IllegalArgumentException("diagnostic field is null");
    }
  }
}
