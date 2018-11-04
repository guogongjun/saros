const SVG = {
  RECT: "rect",
  CIRCLE: "circle",
  LINE: "line",
  ELLIPSE: "ellipse",
  TEXT: "text",
  POLYLINE: "polyline",
  PATH: "path",
  PROPERTIES: {
    X: "x",
    Y: "y",
    R: "r",
    D: "d",
    X1: "x1",
    X2: "x2",
    Y1: "y1",
    Y2: "y2",
    RX: "rx",
    RY: "ry",
    CX: "cx",
    CY: "cy",
    WIDTH: "width",
    HEIGHT: "height",
    TEXT: "text",
    COLOR: "color",
    FILL: "fill",
    FONT_SIZE: "fontSize"
  },
  isNumerical(name) {
    return name in {
      x: 1, x1: 1, x2: 1, y: 1, y1: 1, y2: 1, rx: 1, ry: 1, cx: 1, cy: 1, r: 1, width: 1, height: 1,
    fontSize: 1
    };
  },
  isColor(name) {
    return name in { color: 1, fill: 1 };
  }
};

Object.freeze(SVG);
export default SVG;