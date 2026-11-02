package moon.example.render.element;

public record NodeBox(double x, double y, double width, double height, int level) {
  public double centerX() {
    return x + width / 2;
  }

  public double centerY() {
    return y + height / 2;
  }

  public double right() {
    return x + width;
  }

  public double bottom() {
    return y + height;
  }

  public Point top() {
    return new Point(centerX(), y);
  }

  public Point bottomCenter() {
    return new Point(centerX(), bottom());
  }

  public Point leftCenter() {
    return new Point(x, centerY());
  }
}
