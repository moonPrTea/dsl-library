package moon.example.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SvgDiagramExporter {
  private static final String FONT_FAMILY =
          "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";
  private static final String GRID_PATTERN_ID = "grid";

  public void write(RenderScene scene, Path path) throws IOException {
    Path parent = path.toAbsolutePath().getParent();

    if (parent != null) {
      Files.createDirectories(parent);
    }

    Files.writeString(path, export(scene), StandardCharsets.UTF_8);
  }

  public String export(RenderScene scene) {
    StringBuilder svg = new StringBuilder();

    svg.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<svg xmlns=\"http://www.w3.org/2000/svg\"")
            .append(" width=\"").append(scene.getWidth()).append('"')
            .append(" height=\"").append(scene.getHeight()).append('"')
            .append(" viewBox=\"0 0 ").append(scene.getWidth()).append(' ')
            .append(scene.getHeight()).append('"')
            .append(" data-theme=\"").append(escape(scene.getTheme().name())).append("\">\n");

    if (!isTransparent(scene.getTheme().colors().canvas())) {
      svg.append("  <rect width=\"100%\" height=\"100%\" fill=\"")
              .append(scene.getTheme().colors().canvas()).append("\"/>\n");
    }

    for (Element element : scene.getElements()) {
      svg.append("  ").append(render(element)).append('\n');
    }

    return svg.append("</svg>\n").toString();
  }

  private String render(Element element) {
    return switch (element) {
      case LineGrid grid -> renderLineGrid(grid);
      case DotGrid grid -> renderDotGrid(grid);
      case Rect rect -> "<rect x=\"" + number(rect.x()) + "\" y=\"" + number(rect.y())
              + "\" width=\"" + number(rect.width()) + "\" height=\"" + number(rect.height())
              + "\" rx=\"" + number(rect.radius()) + "\" fill=\"" + rect.fill()
              + "\" stroke=\"" + rect.stroke()
              + "\" stroke-width=\"" + number(rect.strokeWidth()) + "\"/>";
      case Circle circle -> "<circle cx=\"" + number(circle.centerX())
              + "\" cy=\"" + number(circle.centerY()) + "\" r=\"" + number(circle.radius())
              + "\" fill=\"" + circle.fill() + "\" stroke=\"" + circle.stroke()
              + "\" stroke-width=\"" + number(circle.strokeWidth()) + "\"/>";
      case Polygon polygon -> "<polygon points=\"" + points(polygon.points())
              + "\" fill=\"" + polygon.fill() + "\" stroke=\"" + polygon.stroke()
              + "\" stroke-width=\"" + number(polygon.strokeWidth())
              + "\" stroke-linejoin=\"round\"/>";
      case Polyline polyline -> "<polyline points=\"" + points(polyline.points())
              + "\" fill=\"none\" stroke=\"" + polyline.stroke()
              + "\" stroke-width=\"" + number(polyline.strokeWidth())
              + "\" stroke-linejoin=\"round\" stroke-linecap=\"butt\""
              + dashAttribute(polyline.dashed()) + "/>";
      case Curve curve -> "<path d=\"M " + point(curve.start())
              + " C " + point(curve.control1()) + ", " + point(curve.control2())
              + ", " + point(curve.end()) + "\" fill=\"none\" stroke=\"" + curve.stroke()
              + "\" stroke-width=\"" + number(curve.strokeWidth())
              + "\" stroke-linecap=\"butt\"" + dashAttribute(curve.dashed()) + "/>";
      case Text text -> "<text x=\"" + number(text.x()) + "\" y=\"" + number(text.y())
              + "\" fill=\"" + text.color() + "\" font-family=\"" + FONT_FAMILY
              + "\" font-size=\"" + number(text.fontSize())
              + "\" text-anchor=\"" + text.anchor().name().toLowerCase(Locale.ROOT) + '"'
              + (text.bold() ? " font-weight=\"600\"" : "")
              + '>' + escape(text.value()) + "</text>";
      case Icon icon -> renderIcon(icon);
    };
  }

  private String dashAttribute(boolean dashed) {
    return dashed ? " stroke-dasharray=\"1 5\"" : "";
  }

  private String renderIcon(Icon icon) {
    double scale = icon.size() / 24.0;
    StringBuilder svg = new StringBuilder("<g transform=\"translate(")
            .append(number(icon.x())).append(' ').append(number(icon.y()))
            .append(") scale(").append(number(scale)).append(")\" fill=\"none\" stroke=\"")
            .append(icon.color()).append("\" stroke-width=\"2\" stroke-linecap=\"round\""
                    + " stroke-linejoin=\"round\">");

    for (String element : icon.icon().elements()) {
      svg.append(element);
    }

    return svg.append("</g>").toString();
  }

  private String renderLineGrid(LineGrid grid) {
    String spacing = number(grid.spacing());

    return "<defs><pattern id=\"" + GRID_PATTERN_ID + "\" width=\"" + spacing
            + "\" height=\"" + spacing + "\" patternUnits=\"userSpaceOnUse\">"
            + "<path d=\"M " + spacing + " 0 L 0 0 0 " + spacing
            + "\" fill=\"none\" stroke=\"" + grid.color() + "\" stroke-width=\"1\"/>"
            + "</pattern></defs>\n"
            + "  <rect width=\"100%\" height=\"100%\" fill=\"url(#" + GRID_PATTERN_ID + ")\"/>";
  }

  private String renderDotGrid(DotGrid grid) {
    String spacing = number(grid.spacing());

    return "<defs><pattern id=\"" + GRID_PATTERN_ID + "\" width=\"" + spacing
            + "\" height=\"" + spacing + "\" patternUnits=\"userSpaceOnUse\">"
            + "<circle cx=\"" + number(grid.radius()) + "\" cy=\"" + number(grid.radius())
            + "\" r=\"" + number(grid.radius()) + "\" fill=\"" + grid.color() + "\"/>"
            + "</pattern></defs>\n"
            + "  <rect width=\"100%\" height=\"100%\" fill=\"url(#" + GRID_PATTERN_ID + ")\"/>";
  }

  private String points(List<Point> points) {
    return points.stream().map(point -> number(point.x()) + "," + number(point.y()))
            .collect(Collectors.joining(" "));
  }

  private String point(Point point) {
    return number(point.x()) + " " + number(point.y());
  }

  private static String number(double value) {
    if (value == Math.rint(value)) {
      return Long.toString(Math.round(value));
    }

    return String.format(Locale.ROOT, "%.2f", value)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
  }

  private static boolean isTransparent(String color) {
    return color.equalsIgnoreCase("none") || color.equalsIgnoreCase("transparent");
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
  }
}
