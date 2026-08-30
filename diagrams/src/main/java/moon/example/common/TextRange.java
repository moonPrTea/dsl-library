package moon.example.common;

public record TextRange(
        int startIndex,
        int endIndex
) {
  public TextRange {
    if (startIndex < 0) {
      throw new IllegalArgumentException("negative start index: " + startIndex);
    }
    if (endIndex < startIndex) {
      throw new IllegalArgumentException(
              "end index precedes start: " + startIndex + ".." + endIndex);
    }
  }

  public boolean checkEmptiness() {
    return startIndex == endIndex;
  }

  public int length() {
    return endIndex - startIndex;
  }

  public TextRange unionOf(TextRange otherRange) {
    return new TextRange(Math.min(this.startIndex, otherRange.startIndex()),
            Math.max(this.endIndex, otherRange.endIndex()));
  }
}
