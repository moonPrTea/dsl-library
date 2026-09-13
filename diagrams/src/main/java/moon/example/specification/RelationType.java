package moon.example.specification;

import java.util.Set;

public record RelationType(
     String name,
     boolean directed,
     Set<String> requiredAttributes
) {
}
