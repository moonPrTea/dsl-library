package moon.example.render;

import io.brunoborges.jairosvg.JairoSVG;

import java.awt.RenderingHints;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PictureConverter {
  private static final double DEFAULT_SCALE = 3.0;

  private PictureConverter() {
  }

  public static void convert(Path svgPath, Path pngPath) throws Exception {
    convert(svgPath, pngPath, DEFAULT_SCALE);
  }

  public static void convert(Path svgPath, Path pngPath, double scale) throws Exception {
    byte[] png = JairoSVG.builder()
            .fromFile(svgPath)
            .scale(scale)
            .renderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            .renderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            .renderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            .renderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            .renderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            .pngCompressionLevel(9)
            .toPng();

    Files.write(pngPath, png);
  }
}
