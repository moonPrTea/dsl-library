package moon.example.diagnostic;

import moon.example.common.FileInformation;
import moon.example.common.TextPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProblemCollector {
  private static final int PROBLEM_LIMIT = 20;
  private static final int TAB_WIDTH = 4;
  private final FileInformation file;
  private final List<Problem> problems = new ArrayList<>();
  private boolean limitReached = false;

  public ProblemCollector(FileInformation file) {
    this.file = file;
  }

  public void add(Problem problem) {
    if (problems.size() > PROBLEM_LIMIT) {
      limitReached = true;
      return;
    }

    problems.add(problem);
  }

  public boolean hasAnyErrors() {
    return problems.stream()
            .anyMatch(problem -> problem.problemSeverity() == ProblemSeverity.ERROR);
  }

  public boolean hasAnyExceptions() {
    return problems.stream()
            .anyMatch(problem -> problem.problemSeverity() == ProblemSeverity.EXCEPTION);
  }

  public boolean isEmpty() {
    return problems.isEmpty();
  }

  public List<Problem> getAllProblems() {
    List<Problem> sortedProblems = new ArrayList<>(problems);
    sortedProblems
            .sort(Comparator.comparingInt(problem -> problem.range().startIndex()));

    return sortedProblems;
  }

  private static String replaceTabs(String line) {
    return line.replace("\t", " ".repeat(TAB_WIDTH));
  }


  private static String createGutter(int lineIndex) {
    int countDigit = String.valueOf(lineIndex).length();

    return " ".repeat(countDigit);
  }

  private static int getTabsInColumn(String line, int columnNumber) {
    int extra = 0;
    int charsBefore = Math.min(columnNumber - 1, line.length());

    for (int i = 0; i < charsBefore; i++) {
      if (line.charAt(i) == '\t') {
        extra += TAB_WIDTH - 1;
      }
    }

    return columnNumber + extra;
  }

  private static int getCaretLength(Problem problem, String lineContent, int columnNumber) {
    int charsLeftInLine = lineContent.length() - (columnNumber - 1);

    return Math.max(1, Math.min(problem.range().length(), charsLeftInLine));
  }

  private void combineAllDataForProblem(StringBuilder result, Problem problem) {
    int startIndex = problem.range().startIndex();
    TextPosition position = file.getPosition(startIndex);

    int lineNumber = position.line();
    int columnNumber = position.column();

    String lineContent = file.getLineContent(lineNumber);
    String gutter = createGutter(lineNumber);

    String severityLevel = (problem.problemSeverity() == ProblemSeverity.ERROR) ? "error"
            : "warning";

    result.append(severityLevel)
            .append('[').append(problem.code()).append("]: ")
            .append(problem.message())
            .append('\n');

    result.append(gutter).append("--> ")
            .append(file.getFileName()).append(':')
            .append(lineNumber).append(':').append(columnNumber)
            .append('\n');

    result.append(gutter).append(" |\n");

    result.append(lineNumber).append(" | ")
            .append(replaceTabs(lineContent))
            .append('\n');

    int caretColumn = getTabsInColumn(lineContent, columnNumber);
    int caretLength = getCaretLength(problem, lineContent, columnNumber);
    result.append(gutter).append(" | ")
            .append(" ".repeat(caretColumn - 1))
            .append("^".repeat(caretLength))
            .append('\n');

    result.append('\n');
  }

  public String renderProblems() {
    if (problems.isEmpty()) {
      return "";
    }

    StringBuilder outputProblem = new StringBuilder();
    for (Problem problem: getAllProblems()) {
      combineAllDataForProblem(outputProblem, problem);
    }

    addSummary(outputProblem);

    return outputProblem.toString();
  }

  private void addSummary(StringBuilder outputProblem) {
    long countErrors = problems.stream()
            .filter(problem -> problem.problemSeverity() == ProblemSeverity.ERROR)
            .count();
    long countWarnings = problems.size() - countErrors;

    outputProblem.append("found: errors - ").append(countErrors)
            .append(", exceptions - ").append(countWarnings);

    if (limitReached) {
      outputProblem.append(" (showed first ").append(PROBLEM_LIMIT).append(")");
    }
    outputProblem.append('\n');
  }
}
