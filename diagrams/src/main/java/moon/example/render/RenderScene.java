package moon.example.render;

import moon.example.render.diagram.DiagramTheme;
import moon.example.render.element.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RenderScene {
  private final int width;
  private final int height;
  private final DiagramTheme theme;
  private final List<Element> elements = new ArrayList<>();

  public RenderScene(int width, int height, DiagramTheme theme) {
    this.width = width;
    this.height = height;
    this.theme = theme;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public DiagramTheme getTheme() {
    return theme;
  }

  public List<Element> getElements() {
    return Collections.unmodifiableList(elements);
  }

  public void add(Element element) {
    elements.add(element);
  }
}
