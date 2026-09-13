package moon.example.analyzer;

import moon.example.diagnostic.ProblemCollector;
import moon.example.model.Diagram;

public interface ValidationRule {
  void validate(Diagram diagram, ProblemCollector problemCollector);
}
