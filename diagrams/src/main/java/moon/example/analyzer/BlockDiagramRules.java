package moon.example.analyzer;

import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;

import java.util.List;

public final class BlockDiagramRules {

  private BlockDiagramRules() {
  }

  public static List<ValidationRule> all() {
    return List.of(
            BlockDiagramRules::decisionHasBothBranches,
            BlockDiagramRules::branchesStartOnlyFromDecision,
            BlockDiagramRules::terminatorHasAtMostOneOutgoingEdge,
            BlockDiagramRules::diagramHasEntryNode);
  }

  private static void decisionHasBothBranches(Diagram diagram, ProblemCollector problemCollector) {
    for (DiagramNode node : diagram.getNodes()) {
      if (!node.getNodeType().name().equals("Decision")) {
        continue;
      }

      boolean hasYes = hasOutgoingOfType(diagram, node, "Yes");
      boolean hasNo = hasOutgoingOfType(diagram, node, "No");

      if (!hasYes || !hasNo) {
        String missing = !hasYes ? "Yes" : "No";

        problemCollector.add(Problem.error(ProblemCode.DECISION_MISSING_BRANCH,
                node.getDeclarationRange(),
                "decision '" + node.getName() + "' has no '" + missing + "' branch",
                "add " + node.getName() + ".set" + missing + "(...)"));
      }
    }
  }

  private static void branchesStartOnlyFromDecision(Diagram diagram, ProblemCollector problemCollector) {
    for (DiagramEdge edge : diagram.getEdges()) {
      String relationName = edge.getRelationType().name();
      boolean isBranch = relationName.equals("Yes") || relationName.equals("No");

      if (isBranch && !edge.getSource().getNodeType().name().equals("Decision")) {
        problemCollector.add(Problem.error(ProblemCode.BRANCH_NOT_FROM_DECISION,
                edge.getDeclarationRange(),
                "'" + relationName + "' branch may start only from a Decision, but '"
                        + edge.getSource().getName() + "' is a "
                        + edge.getSource().getNodeType().name(),
                "use setNext for non-decision nodes"));
      }
    }
  }

  private static void terminatorHasAtMostOneOutgoingEdge(Diagram diagram, ProblemCollector problemCollector) {
    for (DiagramNode node : diagram.getNodes()) {
      if (!node.getNodeType().name().equals("Terminator")) {
        continue;
      }

      int outgoingCount = diagram.getOutgoingEdges(node).size();

      if (outgoingCount > 1) {
        problemCollector.add(Problem.error(ProblemCode.TERMINATOR_TOO_MANY_OUTGOING,
                node.getDeclarationRange(),
                "terminator '" + node.getName() + "' has " + outgoingCount
                        + " outgoing edges, at most one is allowed"));
      }
    }
  }

  private static void diagramHasEntryNode(Diagram diagram, ProblemCollector problemCollector) {
    if (diagram.getNodes().isEmpty()) {
      return;
    }

    boolean hasEntry = diagram.getNodes().stream()
            .anyMatch(node -> diagram.getIncomingEdges(node).isEmpty());

    if (!hasEntry) {
      problemCollector.add(Problem.error(ProblemCode.NO_ENTRY_NODE,
              diagram.getNodes().get(0).getDeclarationRange(),
              "diagram has no entry node: every node has an incoming edge",
              "a block diagram must have a node without incoming edges"));
    }
  }

  private static boolean hasOutgoingOfType(Diagram diagram, DiagramNode node, String relationName) {
    return diagram.getOutgoingEdges(node).stream()
            .anyMatch(edge -> edge.getRelationType().name().equals(relationName));
  }
}
