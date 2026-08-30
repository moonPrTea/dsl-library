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

  public static Problem error(ProblemCode code, TextRange textRange, String message) {
    return new Problem(ProblemSeverity.ERROR, code, textRange, message, null);
  }

  public static Problem error(ProblemCode code, TextRange textRange, String message, String hint) {
    return new Problem(ProblemSeverity.ERROR, code, textRange, message, hint);
  }

  public static Problem exception(ProblemCode code, TextRange textRange, String message) {
    return new Problem(ProblemSeverity.EXCEPTION, code, textRange, message, null);
  }
}
