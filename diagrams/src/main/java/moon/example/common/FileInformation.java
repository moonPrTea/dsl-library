package moon.example.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class FileInformation {
  private final String fileName;
  private final String fileContent;
  private final int[] lineStartOffsets;

  public FileInformation(String fileName, String content) {
    this.fileName = fileName;
    this.fileContent = formatLineEndings(content);
    this.lineStartOffsets = getLineStartOffsets(this.fileContent);
  }

  public String getFileName() {
    return fileName;
  }

  public String getFileContent() {
    return fileContent;
  }

  public int[] getLineStartOffsets() {
    return lineStartOffsets;
  }

  public static FileInformation readFrom(Path path) throws IOException {
    String content = Files.readString(path, StandardCharsets.UTF_8);

    return new FileInformation(path.getFileName().toString(), content);
  }

  public String getRange(TextRange range) {
    return fileContent.substring(range.startIndex(), range.endIndex());
  }

  private static String formatLineEndings(String content) {
    String formattedContent = content;

    final char BYTE_ORDER_MARK = '\uFEFF';
    if (!formattedContent.isEmpty() && formattedContent.charAt(0) == BYTE_ORDER_MARK) {
      formattedContent = formattedContent.substring(1);
    }

    return formattedContent.replace("\r\n", "\n")
            .replace('\r', '\n');
  }

  private static int[] getLineStartOffsets(String content) {
    int[] offsets = new int[16];
    int count = 0;

    offsets[count++] = 0;

    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n') {
        if (count == offsets.length) {
          offsets = Arrays.copyOf(offsets, offsets.length * 2);
        }
        offsets[count++] = i + 1;
      }
    }

    return Arrays.copyOf(offsets, count);
  }

  public TextPosition getPosition(int offset) {
    int lineIndex = getLineIndex(offset);
    int lineStartOffset = lineStartOffsets[lineIndex - 1];
    int columnNumber = offset - lineStartOffset + 1;

    return new TextPosition(lineIndex, columnNumber);
  }

  public int getLineIndex(int offset) {
    checkOffset(offset);

    int searchedIndex = Arrays.binarySearch(lineStartOffsets, offset);

    if (searchedIndex >= 0) {
      return searchedIndex + 1;
    }
    int insertionIndex = -searchedIndex - 1;
    int lineIndex = insertionIndex - 1;

    return lineIndex + 1;
  }

  public String getLineContent(int lineIndex) {
    checkLineIndex(lineIndex);

    int startLineOffset = lineStartOffsets[lineIndex - 1];
    boolean isLastLine = (lineIndex == lineStartOffsets.length);

    int endLineIndex = isLastLine ? fileContent.length()
            : lineStartOffsets[lineIndex] - 1;

    return fileContent.substring(startLineOffset, endLineIndex);
  }

  private void checkOffset(int offset) {
    if (offset < 0 || offset > fileContent.length()) {
      throw new IndexOutOfBoundsException("offset " + offset + " is outside file "
              + fileName + " (length " + fileContent.length() + ")");
    }
  }

  private void checkLineIndex(int lineIndex) {
    if (lineIndex < 1 || lineIndex > lineStartOffsets.length) {
      throw new IndexOutOfBoundsException("no line " + lineIndex + " in file "
              + fileName + " (line count " + lineStartOffsets.length + ")");
    }
  }
}
