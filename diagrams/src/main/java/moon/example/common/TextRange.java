package moon.example.common;

public record TextRange(
        int startIndex,
        int endIndex
) {
  public TextRange {
    if ((startIndex < 0) || (endIndex > startIndex)) {
      throw new IllegalArgumentException("Incorrect indexes for text range");
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
            Math.min(this.endIndex, otherRange.endIndex));
  }
}
