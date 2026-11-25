import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// Draws the icon as PNG files: the head of Fiore the Boar, read from the master, on black with no transparency.
// A home screen takes no SVG icon. Run by derive.py:
//     Png.java [--check] fiore.svg (file pixels x y side)...
// Each file shows the square of the master that starts at x,y and is side units wide.
// With --check nothing is written, and a file whose pixels differ from what would be written is named.
// Pixels are compared, not bytes: the bytes of a PNG depend on the compression library of the machine.
// Draws the shapes and attributes the master uses and fails on any other, so a new one is not drawn wrong.
public class Png {
    static double[] numbers(String s) {
        return Arrays.stream(s.trim().split("[\\s,]+")).filter(t -> !t.isEmpty()).mapToDouble(Double::parseDouble).toArray();
    }

    static Path2D path(String d, boolean polygon) {
        Path2D p = new Path2D.Double();
        if (polygon) {
            double[] c = numbers(d);
            p.moveTo(c[0], c[1]);
            for (int i = 2; i < c.length; i += 2) p.lineTo(c[i], c[i + 1]);
            p.closePath();
            return p;
        }
        Matcher m = Pattern.compile("(.)([^A-Za-z]*)").matcher(d);
        while (m.find()) {
            double[] c = numbers(m.group(2));
            String command = m.group(1) + c.length;
            switch (command) {
                case "M2" -> p.moveTo(c[0], c[1]);
                case "L2" -> p.lineTo(c[0], c[1]);
                case "Q4" -> p.quadTo(c[0], c[1], c[2], c[3]);
                case "Z0" -> p.closePath();
                default -> throw new IllegalArgumentException("path command " + m.group(0));
            }
        }
        return p;
    }

    static double number(Element e, String name) { return Double.parseDouble(e.getAttribute(name)); }

    static Shape shape(Element e) {
        switch (e.getTagName()) {
            case "polygon": return path(e.getAttribute("points"), true);
            case "path": return path(e.getAttribute("d"), false);
            case "circle": case "ellipse":
                boolean circle = e.getTagName().equals("circle");
                double rx = number(e, circle ? "r" : "rx"), ry = number(e, circle ? "r" : "ry");
                return new Ellipse2D.Double(number(e, "cx") - rx, number(e, "cy") - ry, 2 * rx, 2 * ry);
            default: throw new IllegalArgumentException("shape " + e.getTagName());
        }
    }

    static final Set<String> ATTRIBUTES = Set.of("points", "d", "cx", "cy", "r", "rx", "ry", "fill", "stroke", "stroke-width", "stroke-linecap");

    static void draw(Graphics2D g, Element e) {
        for (int i = 0; i < e.getAttributes().getLength(); i++) {
            String name = e.getAttributes().item(i).getNodeName();
            if (!ATTRIBUTES.contains(name)) throw new IllegalArgumentException("attribute " + name);
        }
        Shape s = shape(e);
        if (!e.getAttribute("fill").equals("none")) {
            g.setColor(Color.decode(e.getAttribute("fill")));
            g.fill(s);
        }
        if (e.hasAttribute("stroke")) {
            int cap = switch (e.getAttribute("stroke-linecap")) {
                case "" -> BasicStroke.CAP_BUTT;
                case "round" -> BasicStroke.CAP_ROUND;
                default -> throw new IllegalArgumentException("stroke-linecap " + e.getAttribute("stroke-linecap"));
            };
            // SVG joins strokes with a mitre, limit 4, unless told otherwise.
            g.setStroke(new BasicStroke((float) number(e, "stroke-width"), cap, BasicStroke.JOIN_MITER, 4f));
            g.setColor(Color.decode(e.getAttribute("stroke")));
            g.draw(s);
        }
    }

    public static void main(String[] a) throws Exception {
        boolean check = a[0].equals("--check");
        int first = check ? 1 : 0;
        NodeList groups = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(a[first])).getElementsByTagName("g");
        Element head = null;
        for (int i = 0; i < groups.getLength(); i++) {
            if (((Element) groups.item(i)).getAttribute("id").equals("fiore-head")) head = (Element) groups.item(i);
        }
        boolean stale = false;
        for (int i = first + 1; i < a.length; i += 5) {
            File file = new File(a[i]);
            int pixels = Integer.parseInt(a[i + 1]);
            double scale = pixels / Double.parseDouble(a[i + 4]);
            BufferedImage image = new BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB); // black, and no transparency
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.scale(scale, scale);
            g.translate(-Double.parseDouble(a[i + 2]), -Double.parseDouble(a[i + 3]));
            for (Node n = head.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element e) draw(g, e);
            }
            if (!check) {
                ImageIO.write(image, "png", file);
                continue;
            }
            BufferedImage written = file.exists() ? ImageIO.read(file) : null;
            if (written == null || written.getWidth() != pixels || written.getHeight() != pixels
                    || !Arrays.equals(written.getRGB(0, 0, pixels, pixels, null, 0, pixels), image.getRGB(0, 0, pixels, pixels, null, 0, pixels))) {
                System.err.println(file + " differs from what scripts/brand/derive.py writes");
                stale = true;
            }
        }
        if (stale) System.exit(1);
    }
}
