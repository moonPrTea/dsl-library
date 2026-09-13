package moon.example.model;

public record Color(int red, int green, int blue) {
  public static final Color WHITE = new Color(255, 255, 255);

  public String toHex() {
    return "#%02X%02X%02X".formatted(red, green, blue);
  }
}
