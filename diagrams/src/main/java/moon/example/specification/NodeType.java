package moon.example.specification;

import java.util.Map;

public record NodeType(
     String name,
     Shape shape,
     Map<String, PropertyType> properties
) {
  public boolean hasProperty(String methodName) {
    return properties.containsKey(methodName);
  }

  public PropertyType getPropertyType(String methodName) {
    return properties.get(methodName);
  }
}
