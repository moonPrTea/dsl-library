package moon.example.diagnostic;

import moon.example.common.FileInformation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProblemCollector {
  private static final int PROBLEM_LIMIT = 20;
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
}
