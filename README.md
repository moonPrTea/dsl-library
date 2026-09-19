# diagram-dsl

A Java DSL for describing block diagrams (`.bck` files), with a lexer, parser, semantic
analyzer, and an SVG renderer that ships two visual themes

## Quick Start

### 1. Run the analyzer (prints the parsed model)

```bash
cd diagrams
./mvnw -q package
java -cp target/classes moon.example.Main examples/example.bck
```

### 2. Render an SVG diagram

```bash
java -cp target/classes moon.example.Main examples/example.bck --theme default
java -cp target/classes moon.example.Main examples/example.bck --theme custom
```

Without `--theme`/`--output`/`--selected`, the command just prints the analyzed model instead
of rendering. Full options:

```text
Usage: java moon.example.Main [input.bck] [options]
  --theme default|custom   rendering theme (default: default)
  --output, -o PATH        output SVG file (default: <input>-<theme>.svg next to the input)
  --selected NODE          render one node as selected
  --help, -h               show this help
```

## Themes

- **`default`** — a GOST-style flowchart: rounded terminators, rectangular processes, diamond
  decisions, orthogonal black connectors

  ![default theme example](https://github.com/moonPrTea/dsl-library/blob/main/diagrams/examples/example-default.svg)

- **`custom`** — a light no-code-automation-builder look: rounded white cards, colored category
  badges with Lucide icons, and curved branch forks

  ![custom theme example](https://github.com/moonPrTea/dsl-library/blob/main/diagrams/examples/example-custom.svg)

## Writing a diagram

```java
BlockDiagram diagram = new BlockDiagram();
diagram.setTitle("Login flow");

Terminator start = new Terminator();
start.setTitle("Open login page");

Decision check = new Decision();
check.setTitle("Right credentials?");

start.setNext(check);
diagram.addBlock(start);
diagram.addBlock(check);
```

See [`examples/example.bck`](https://github.com/moonPrTea/dsl-library/tree/main/diagrams/examples) for a complete diagram with a
branch (`setYes`/`setNo`)
