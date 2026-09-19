package moon.example.render;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PictureConverterTest {
  @Test
  void testConvertShouldWritePngWithMatchingDimensions(@TempDir Path tempDir) throws Exception {
    Path svgPath = tempDir.resolve("diagram.svg");
    Files.writeString(svgPath, """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="120" height="80"
                 viewBox="0 0 120 80">
              <rect width="100%" height="100%" fill="#FFFFFF"/>
              <circle cx="60" cy="40" r="20" fill="#111827"/>
            </svg>
            """);
    Path pngPath = tempDir.resolve("diagram.png");

    PictureConverter.convert(svgPath, pngPath, 1.0);

    assertTrue(Files.exists(pngPath));
    assertTrue(Files.size(pngPath) > 0);
    assertEquals(120, readPngWidth(pngPath));
    assertEquals(80, readPngHeight(pngPath));
  }

  @Test
  void testConvertShouldScaleOutputDimensions(@TempDir Path tempDir) throws Exception {
    Path svgPath = tempDir.resolve("diagram.svg");
    Files.writeString(svgPath, """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg xmlns="http://www.w3.org/2000/svg" width="100" height="50"
                 viewBox="0 0 100 50">
              <rect width="100%" height="100%" fill="#FFFFFF"/>
            </svg>
            """);
    Path pngPath = tempDir.resolve("diagram@2x.png");

    PictureConverter.convert(svgPath, pngPath, 2.0);

    assertEquals(200, readPngWidth(pngPath));
    assertEquals(100, readPngHeight(pngPath));
  }

  private static int readPngWidth(Path pngPath) throws Exception {
    byte[] header = readPngHeaderBytes(pngPath);
    return ((header[16] & 0xFF) << 24) | ((header[17] & 0xFF) << 16)
            | ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
  }

  private static int readPngHeight(Path pngPath) throws Exception {
    byte[] header = readPngHeaderBytes(pngPath);
    return ((header[20] & 0xFF) << 24) | ((header[21] & 0xFF) << 16)
            | ((header[22] & 0xFF) << 8) | (header[23] & 0xFF);
  }

  private static byte[] readPngHeaderBytes(Path pngPath) throws Exception {
    byte[] bytes = Files.readAllBytes(pngPath);
    return Arrays.copyOfRange(bytes, 0, 24);
  }
}
