package moon.example.analyzer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SymbolTable {
  private final Map<String, Symbol> symbols = new LinkedHashMap<>();

  public boolean contains(String name) {
    return symbols.containsKey(name);
  }

  public Symbol find(String name) {
    return symbols.get(name);
  }

  public void define(Symbol symbol) {
    symbols.put(symbol.name(), symbol);
  }

  public Collection<String> getNames() {
    return symbols.keySet();
  }
}
