package moon.example.specification;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SpecificationRegistry {
  private final Map<String, DiagramSpecification> specifications = new LinkedHashMap<>();

  public SpecificationRegistry() {
    register(new BlockDiagramSpecification());
  }

  private void register(DiagramSpecification specification) {
    specifications.put(specification.getDiagramTypeName(), specification);
  }

  public DiagramSpecification find(String diagramTypeName) {
    return specifications.get(diagramTypeName);
  }

  public boolean isDiagramType(String name) {
    return specifications.containsKey(name);
  }

  public Collection<String> getDiagramTypeNames() {
    return specifications.keySet();
  }
}
