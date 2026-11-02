package moon.example.analyzer;

import moon.example.ast.Argument;
import moon.example.ast.Call;
import moon.example.ast.Declaration;
import moon.example.ast.NumberLiteral;
import moon.example.ast.Program;
import moon.example.ast.QualifiedName;
import moon.example.ast.Statement;
import moon.example.ast.StringLiteral;
import moon.example.ast.VariableReference;
import moon.example.common.TextRange;
import moon.example.diagnostic.Problem;
import moon.example.diagnostic.ProblemCode;
import moon.example.diagnostic.ProblemCollector;
import moon.example.model.Color;
import moon.example.model.Diagram;
import moon.example.model.DiagramEdge;
import moon.example.model.DiagramNode;
import moon.example.specification.DiagramSpecification;
import moon.example.specification.DiagramLucideIcons;
import moon.example.specification.NodeType;
import moon.example.specification.PropertyType;
import moon.example.specification.RelationType;
import moon.example.specification.SpecificationRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Analyzer {
  private final SpecificationRegistry registry;
  private final ProblemCollector problemCollector;

  private final SymbolTable symbolTable = new SymbolTable();
  private final Map<String, DiagramNode> nodesByName = new LinkedHashMap<>();
  private final Map<String, PendingRelation> relationsByName = new LinkedHashMap<>();
  private final Set<String> explicitlyAddedNodes = new LinkedHashSet<>();
  private final List<DiagramEdge> edges = new ArrayList<>();

  private DiagramSpecification specification;
  private Diagram diagram;
  private String diagramVariableName;

  public Analyzer(SpecificationRegistry registry, ProblemCollector problemCollector) {
    this.registry = registry;
    this.problemCollector = problemCollector;
  }

  public Diagram analyze(Program program) {
    collectDeclarations(program);

    if (diagram == null) {
      return null;
    }

    processCalls(program);
    assembleDiagram();
    validateDiagram();

    return diagram;
  }

  private void collectDeclarations(Program program) {
    if (!declaresAnyDiagram(program)) {
      TextRange range = program.statements().isEmpty()
              ? new TextRange(0, 0)
              : program.statements().getFirst().textRange();

      problemCollector.add(Problem.error(ProblemCode.MISSING_DIAGRAM, range,
              "no diagram is declared in the file",
              "start the file with 'BlockDiagram diagram = new BlockDiagram();'"));
      return;
    }

    for (Statement statement : program.statements()) {
      if (statement instanceof Declaration declaration) {
        collectDeclaration(declaration);
      }
    }
  }

  private boolean declaresAnyDiagram(Program program) {
    return program.statements().stream()
            .anyMatch(statement -> statement instanceof Declaration declaration
                    && registry.isDiagramType(declaration.typeName()));
  }

  private void collectDeclaration(Declaration declaration) {
    if (symbolTable.contains(declaration.variableName())) {
      reportDuplicateDeclaration(declaration);
      return;
    }

    if (!declaration.typeName().equals(declaration.constructorTypeName())) {
      problemCollector.add(Problem.error(ProblemCode.TYPE_MISMATCH,
              declaration.constructorTypeNameRange(),
              "variable '" + declaration.variableName() + "' is declared as '"
                      + declaration.typeName() + "' but created as '"
                      + declaration.constructorTypeName() + "'",
              "use the same type on both sides: " + declaration.typeName()
                      + " " + declaration.variableName() + " = new "
                      + declaration.typeName() + "();"));
      return;
    }

    String typeName = declaration.typeName();

    if (registry.isDiagramType(typeName)) {
      collectDiagramDeclaration(declaration);
      return;
    }

    if (specification == null) {
      problemCollector.add(Problem.error(ProblemCode.DIAGRAM_NOT_FIRST,
              declaration.textRange(),
              "'" + typeName + "' is declared before the diagram",
              "the diagram declaration must be the first statement in the file"));
      return;
    }

    NodeType nodeType = specification.findNodeType(typeName);
    if (nodeType != null) {
      DiagramNode node = new DiagramNode(declaration.variableName(), nodeType, declaration.textRange());

      nodesByName.put(node.getName(), node);
      symbolTable.define(new Symbol(node.getName(), SymbolKind.NODE, typeName, declaration.textRange()));
      return;
    }

    RelationType relationType = specification.findRelationType(typeName);
    if (relationType != null) {
      relationsByName.put(declaration.variableName(),
              new PendingRelation(relationType, declaration.textRange()));
      symbolTable.define(new Symbol(declaration.variableName(), SymbolKind.RELATION, typeName,
              declaration.textRange()));
      return;
    }

    reportUnknownType(declaration);
  }

  private void collectDiagramDeclaration(Declaration declaration) {
    if (diagram != null) {
      problemCollector.add(Problem.error(ProblemCode.MULTIPLE_DIAGRAMS,
              declaration.textRange(),
              "only one diagram per file is allowed, '" + diagramVariableName
                      + "' is already declared"));
      return;
    }

    if (!symbolTable.getNames().isEmpty()) {
      problemCollector.add(Problem.error(ProblemCode.DIAGRAM_NOT_FIRST,
              declaration.textRange(),
              "the diagram must be declared before any other variable",
              "move this line to the top of the file"));
    }

    specification = registry.find(declaration.typeName());
    diagram = new Diagram(specification);
    diagramVariableName = declaration.variableName();
    symbolTable.define(new Symbol(diagramVariableName, SymbolKind.DIAGRAM,
            declaration.typeName(), declaration.textRange()));
  }

  private void processCalls(Program program) {
    for (Statement statement : program.statements()) {
      if (statement instanceof Call call) {
        processCall(call);
      }
    }
  }

  private void processCall(Call call) {
    Symbol receiver = symbolTable.find(call.receiverName());

    if (receiver == null) {
      reportUnknownVariable(call.receiverName(), call.receiverNameRange());
      return;
    }

    switch (receiver.kind()) {
      case DIAGRAM -> processDiagramCall(call);
      case NODE -> processNodeCall(call, nodesByName.get(receiver.name()));
      case RELATION -> processRelationCall(call, relationsByName.get(receiver.name()));
    }
  }

  private void processDiagramCall(Call call) {
    String method = call.methodName();

    switch (method) {
      case "addBlock" -> {
        DiagramNode node = expectNodeArgument(call, 0);

        if (node != null) {
          explicitlyAddedNodes.add(node.getName());
        }
      }
      case "addRelation" -> {
        PendingRelation relation = expectRelationArgument(call, 0);

        if (relation != null) {
          relation.added = true;
        }
      }
      default -> {
        PropertyType propertyType = specification.findDiagramProperty(method);

        if (propertyType == null) {
          List<String> knownMethods = new ArrayList<>(specification.getDiagramMethodNames());
          knownMethods.add("addBlock");
          knownMethods.add("addRelation");
          reportUnknownMethod(call, "diagram", knownMethods);
          return;
        }

        applyDiagramProperty(call, propertyType);
      }
    }
  }

  private void applyDiagramProperty(Call call, PropertyType propertyType) {
    if (!expectArgumentCount(call, 1)) {
      return;
    }

    Argument argument = call.arguments().get(0);

    switch (call.methodName()) {
      case "setTitle" -> {
        String value = expectString(argument, propertyType);
        if (value != null) {
          diagram.setTitle(value);
        }
      }
      case "setDescription" -> {
        String value = expectString(argument, propertyType);
        if (value != null) {
          diagram.setDescription(value);
        }
      }
      case "setColor" -> {
        Color color = expectColor(argument);
        if (color != null) {
          diagram.setBackgroundColor(color);
        }
      }
      default -> throw new IllegalStateException("unhandled diagram property " + call.methodName());
    }
  }

  private void processNodeCall(Call call, DiagramNode node) {
    String method = call.methodName();
    NodeType nodeType = node.getNodeType();

    if (nodeType.hasProperty(method)) {
      applyNodeProperty(call, node, nodeType.getPropertyType(method));
      return;
    }

    RelationType relationType = specification.findRelationSugar(method);

    if (relationType != null) {
      if (!expectArgumentCount(call, 1)) {
        return;
      }

      DiagramNode target = expectNodeArgument(call, 0);

      if (target != null) {
        edges.add(new DiagramEdge(relationType, node, target, call.textRange()));
      }
      return;
    }

    List<String> knownMethods = new ArrayList<>(nodeType.properties().keySet());
    knownMethods.addAll(specification.getRelationSugarMethodNames());
    reportUnknownMethod(call, "type '" + nodeType.name() + "'", knownMethods);
  }

  private void applyNodeProperty(Call call, DiagramNode node, PropertyType propertyType) {
    if (!expectArgumentCount(call, 1)) {
      return;
    }

    Argument argument = call.arguments().get(0);

    switch (call.methodName()) {
      case "setTitle" -> {
        String value = expectString(argument, propertyType);
        if (value != null) {
          node.setTitle(value);
        }
      }
      case "setBadgeLabel" -> {
        String value = expectString(argument, propertyType);
        if (value != null) {
          node.setBadgeLabel(value);
        }
      }
      case "setBadgeIcon" -> {
        String value = expectString(argument, propertyType);
        if (value != null && validateBadgeIcon(value, argument.textRange())) {
          node.setBadgeIcon(value);
        }
      }
      case "setBadgeCategory" -> {
        String value = expectString(argument, propertyType);
        if (value != null) {
          node.setBadgeCategory(value);
        }
      }
      default -> throw new IllegalStateException("unhandled node property " + call.methodName());
    }
  }

  private boolean validateBadgeIcon(String value, TextRange range) {
    if (DiagramLucideIcons.find(value) != null) {
      return true;
    }

    String hint = NameSuggester.suggestionHint(
            value.toLowerCase(Locale.ROOT), DiagramLucideIcons.names());
    if (hint == null) {
      hint = "available icons: " + String.join(", ", DiagramLucideIcons.names());
    }

    problemCollector.add(Problem.error(ProblemCode.UNKNOWN_BADGE_ICON, range,
            "unknown badge icon '" + value + "'", hint));
    return false;
  }

  private void processRelationCall(Call call, PendingRelation relation) {
    String method = call.methodName();

    switch (method) {
      case "setFrom" -> relation.source = expectNodeArgument(call, 0);
      case "setTo" -> relation.target = expectNodeArgument(call, 0);
      case "setLabel" -> {
        if (expectArgumentCount(call, 1)) {
          String value = expectString(call.arguments().get(0), PropertyType.STRING);
          if (value != null) {
            relation.label = value;
          }
        }
      }
      default -> reportUnknownMethod(call, "relation", List.of("setFrom", "setTo", "setLabel"));
    }
  }

  private void assembleDiagram() {
    for (Map.Entry<String, PendingRelation> entry : relationsByName.entrySet()) {
      PendingRelation relation = entry.getValue();

      if (!relation.added) {
        if (!problemCollector.hasAnyErrors()) {
          problemCollector.add(Problem.warning(ProblemCode.RELATION_NOT_ADDED,
                relation.declarationRange,
                "relation '" + entry.getKey() + "' is declared but never added to the diagram",
                "add " + diagramVariableName + ".addRelation(" + entry.getKey() + ");"));
        }
        continue;
      }

      if (relation.source == null || relation.target == null) {
        problemCollector.add(Problem.error(ProblemCode.RELATION_INCOMPLETE,
                relation.declarationRange,
                "relation '" + entry.getKey() + "' must have both setFrom and setTo"));
        continue;
      }

      DiagramEdge edge = new DiagramEdge(relation.relationType, relation.source, relation.target,
              relation.declarationRange);
      edge.setLabel(relation.label);
      edges.add(edge);
    }

    Set<DiagramNode> connectedNodes = new LinkedHashSet<>();
    for (DiagramEdge edge : edges) {
      connectedNodes.add(edge.getSource());
      connectedNodes.add(edge.getTarget());
    }

    boolean reportWarnings = !problemCollector.hasAnyErrors();

    for (DiagramNode node : nodesByName.values()) {
      boolean isConnected = connectedNodes.contains(node);
      boolean isExplicitlyAdded = explicitlyAddedNodes.contains(node.getName());

      if (isConnected || isExplicitlyAdded) {
        diagram.addNode(node);
      } else if (reportWarnings) {
        problemCollector.add(Problem.warning(ProblemCode.ORPHAN_NODE,
                node.getDeclarationRange(),
                "node '" + node.getName() + "' is not connected to the diagram",
                "connect it with an edge or add it with "
                        + diagramVariableName + ".addBlock(" + node.getName() + ")"));
      }
    }

    for (int i = 0; i < edges.size(); i++) {
      DiagramEdge edge = edges.get(i);
      boolean isDuplicate = false;

      for (int j = 0; j < i; j++) {
        if (edge.connectsSameNodesAs(edges.get(j))) {
          isDuplicate = true;
          break;
        }
      }

      if (isDuplicate) {
        if (!reportWarnings) {
          continue;
        }

        problemCollector.add(Problem.warning(ProblemCode.DUPLICATE_EDGE,
                edge.getDeclarationRange(),
                "duplicate edge " + edge));
      } else {
        diagram.addEdge(edge);
      }
    }
  }

  private void validateDiagram() {
    if (problemCollector.hasAnyErrors()) {
      return;
    }

    for (ValidationRule rule : rulesFor(specification)) {
      rule.validate(diagram, problemCollector);
    }
  }

  private static List<ValidationRule> rulesFor(DiagramSpecification specification) {
    return switch (specification.getDiagramTypeName()) {
      case "BlockDiagram" -> BlockDiagramRules.all();
      default -> List.of();
    };
  }

  private DiagramNode expectNodeArgument(Call call, int index) {
    if (!expectArgumentCount(call, 1)) {
      return null;
    }

    Argument argument = call.arguments().get(index);

    if (!(argument instanceof VariableReference reference)) {
      problemCollector.add(Problem.error(ProblemCode.NOT_A_NODE, argument.textRange(),
              "expected a node variable but found " + describe(argument)));
      return null;
    }

    Symbol symbol = symbolTable.find(reference.name());

    if (symbol == null) {
      reportUnknownVariable(reference.name(), reference.textRange());
      return null;
    }

    if (symbol.kind() != SymbolKind.NODE) {
      problemCollector.add(Problem.error(ProblemCode.NOT_A_NODE, argument.textRange(),
              "'" + reference.name() + "' is a " + symbol.kind().name().toLowerCase()
                      + ", but a node is expected here"));
      return null;
    }

    return nodesByName.get(reference.name());
  }

  private PendingRelation expectRelationArgument(Call call, int index) {
    if (!expectArgumentCount(call, 1)) {
      return null;
    }

    Argument argument = call.arguments().get(index);

    if (!(argument instanceof VariableReference reference)) {
      problemCollector.add(Problem.error(ProblemCode.NOT_A_RELATION, argument.textRange(),
              "expected a relation variable but found " + describe(argument)));
      return null;
    }

    Symbol symbol = symbolTable.find(reference.name());

    if (symbol == null) {
      reportUnknownVariable(reference.name(), reference.textRange());
      return null;
    }

    if (symbol.kind() != SymbolKind.RELATION) {
      problemCollector.add(Problem.error(ProblemCode.NOT_A_RELATION, argument.textRange(),
              "'" + reference.name() + "' is a " + symbol.kind().name().toLowerCase()
                      + ", but a relation is expected here"));
      return null;
    }

    return relationsByName.get(reference.name());
  }

  private String expectString(Argument argument, PropertyType propertyType) {
    if (argument instanceof StringLiteral literal) {
      return literal.value();
    }

    if (propertyType == PropertyType.NULLABLE_STRING && isDefaultNull(argument)) {
      return "";
    }

    problemCollector.add(Problem.error(ProblemCode.ARGUMENT_TYPE_MISMATCH, argument.textRange(),
            "expected a string but found " + describe(argument)));

    return null;
  }

  private Color expectColor(Argument argument) {
    if (!(argument instanceof QualifiedName qualifiedName)
            || !qualifiedName.qualifier().equals("Colors")
            || !qualifiedName.member().equals("new")) {
      problemCollector.add(Problem.error(ProblemCode.ARGUMENT_TYPE_MISMATCH, argument.textRange(),
              "expected a color but found " + describe(argument),
              "use Colors::new(red, green, blue) with values from 0 to 255"));
      return null;
    }

    List<Argument> components = qualifiedName.arguments();

    if (components.size() != 3) {
      problemCollector.add(Problem.error(ProblemCode.ARGUMENT_COUNT_MISMATCH, argument.textRange(),
              "Colors::new expects 3 components but got " + components.size()));
      return null;
    }

    int[] values = new int[3];

    for (int i = 0; i < 3; i++) {
      Integer value = expectColorComponent(components.get(i));

      if (value == null) {
        return null;
      }

      values[i] = value;
    }

    return new Color(values[0], values[1], values[2]);
  }

  private Integer expectColorComponent(Argument argument) {
    if (!(argument instanceof NumberLiteral literal)) {
      problemCollector.add(Problem.error(ProblemCode.ARGUMENT_TYPE_MISMATCH, argument.textRange(),
              "color component must be a number but found " + describe(argument)));
      return null;
    }

    int value;

    try {
      value = Integer.parseInt(literal.text());
    } catch (NumberFormatException exception) {
      value = Integer.MAX_VALUE;
    }

    if (value < 0 || value > 255) {
      problemCollector.add(Problem.error(ProblemCode.COLOR_OUT_OF_RANGE, argument.textRange(),
              "color component " + literal.text() + " is out of range 0..255"));
      return null;
    }

    return value;
  }

  private boolean expectArgumentCount(Call call, int expectedCount) {
    int actualCount = call.arguments().size();

    if (actualCount == expectedCount) {
      return true;
    }

    problemCollector.add(Problem.error(ProblemCode.ARGUMENT_COUNT_MISMATCH, call.textRange(),
            call.methodName() + " expects " + expectedCount + " argument"
                    + (expectedCount == 1 ? "" : "s") + " but got " + actualCount));

    return false;
  }

  private static boolean isDefaultNull(Argument argument) {
    return argument instanceof QualifiedName qualifiedName
            && qualifiedName.qualifier().equals("default")
            && qualifiedName.member().equals("null");
  }

  private void reportDuplicateDeclaration(Declaration declaration) {
    Symbol existing = symbolTable.find(declaration.variableName());

    problemCollector.add(Problem.error(ProblemCode.DUPLICATE_DECLARATION,
            declaration.variableNameRange(),
            "variable '" + declaration.variableName() + "' is already declared",
            "first declaration is at offset " + existing.declarationRange().startIndex()));
  }

  private void reportUnknownType(Declaration declaration) {
    List<String> knownTypes = new ArrayList<>(registry.getDiagramTypeNames());
    specification.getNodeTypes().forEach(nodeType -> knownTypes.add(nodeType.name()));
    specification.getRelationTypes().forEach(relationType -> knownTypes.add(relationType.name()));

    problemCollector.add(Problem.error(ProblemCode.UNKNOWN_TYPE, declaration.typeNameRange(),
            "unknown type '" + declaration.typeName() + "'",
            NameSuggester.suggestionHint(declaration.typeName(), knownTypes)));
  }

  private void reportUnknownVariable(String name, TextRange range) {
    problemCollector.add(Problem.error(ProblemCode.UNKNOWN_VARIABLE, range,
            "unknown variable '" + name + "'",
            NameSuggester.suggestionHint(name, symbolTable.getNames())));
  }

  private void reportUnknownMethod(Call call, String receiverDescription, List<String> knownMethods) {
    String suggestion = NameSuggester.suggestionHint(call.methodName(), knownMethods);
    String hint = (suggestion != null)
            ? suggestion
            : "available methods: " + String.join(", ", knownMethods);

    problemCollector.add(Problem.error(ProblemCode.UNKNOWN_METHOD, call.methodNameRange(),
            receiverDescription + " does not support method '" + call.methodName() + "'",
            hint));
  }

  private static String describe(Argument argument) {
    return switch (argument) {
      case StringLiteral literal -> "string \"" + literal.value() + "\"";
      case NumberLiteral literal -> "number " + literal.text();
      case VariableReference reference -> "variable '" + reference.name() + "'";
      case QualifiedName qualifiedName -> qualifiedName.qualifier() + "::" + qualifiedName.member();
    };
  }

  private static final class PendingRelation {
    final RelationType relationType;
    final TextRange declarationRange;
    DiagramNode source;
    DiagramNode target;
    String label = "";
    boolean added = false;

    PendingRelation(RelationType relationType, TextRange declarationRange) {
      this.relationType = relationType;
      this.declarationRange = declarationRange;
    }
  }
}
