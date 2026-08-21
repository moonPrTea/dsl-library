package moon.example.diagnostic;

import moon.example.common.TextRange;

public record Problem(
        ProblemSeverity problemSeverity,
        ProblemCode code,
        TextRange range,
        String message,
        String hint
) {
  public Problem {
    if (problemSeverity == null || code == null || range == null || message == null) {
      throw new IllegalArgumentException("diagnostic field is null");
    }
  }
}
