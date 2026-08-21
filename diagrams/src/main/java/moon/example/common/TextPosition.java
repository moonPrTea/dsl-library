package moon.example.common;

public record TextPosition(
        int line,
        int column
) {
  @Override
  public String toString() {
    return line + ":" + column;
  }
}
