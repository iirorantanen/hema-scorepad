import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.*;

// Prints the path of fiore-mono, the one-colour form of Fiore the Boar: one outline with transparent holes.
// A themed icon is drawn in a single colour, so a gap cannot be painted in a background colour.
// Run by derive.py. Rendering code is not needed: java.awt.geom works without a display.
// shortcut: the shapes are typed here again, not read from docs/brand/fiore.svg; read them from the file once the head changes shape.
public class Mono {
    static Path2D poly(double... c) {
        Path2D p = new Path2D.Double();
        p.moveTo(c[0], c[1]);
        for (int i = 2; i < c.length; i += 2) p.lineTo(c[i], c[i + 1]);
        p.closePath();
        return p;
    }
    static Path2D quads(double... c) { // M x,y then Q cx,cy x,y pairs, closed
        Path2D p = new Path2D.Double();
        p.moveTo(c[0], c[1]);
        for (int i = 2; i < c.length; i += 4) p.quadTo(c[i], c[i + 1], c[i + 2], c[i + 3]);
        p.closePath();
        return p;
    }
    static Shape ell(double cx, double cy, double rx, double ry) { return new Ellipse2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry); }
    static void outlined(Area r, Shape s, float w) { // filled, then separated from what is under it by a gap
        r.add(new Area(s));
        r.subtract(new Area(new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4f).createStrokedShape(s)));
    }
    static String n(double v) {
        String s = String.format(java.util.Locale.ROOT, "%.1f", v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
    public static void main(String[] a) {
        float gap = 5; // the width of the gap around the cap, the snout and the tusks
        Area r = new Area();
        r.add(new Area(poly(112,92, 120,60, 130,88, 140,54, 150,86, 160,54, 170,88, 180,60, 188,92)));
        r.add(new Area(poly(78,110, 70,62, 112,92)));
        r.add(new Area(poly(222,110, 230,62, 188,92)));
        r.add(new Area(ell(150,150,82,70)));
        outlined(r, quads(88,104, 150,40, 212,104, 150,88, 88,104), gap);
        r.add(new Area(quads(200,90, 250,40, 262,70, 235,70, 205,98)));
        outlined(r, ell(150,188,40,27), gap);
        r.subtract(new Area(ell(137,188,6,9)));
        r.subtract(new Area(ell(163,188,6,9)));
        r.subtract(new Area(ell(124,139,9,9)));
        r.subtract(new Area(ell(176,139,9,9)));
        outlined(r, quads(116,203, 96,198, 98,172, 108,192, 124,196), gap);
        outlined(r, quads(184,203, 204,198, 202,172, 192,192, 176,196), gap);
        // Rounded to one decimal; segments and subpaths that the rounding leaves without length are dropped.
        StringBuilder d = new StringBuilder();
        StringBuilder sub = new StringBuilder();
        int drawn = 0;
        String at = "";
        double[] c = new double[6];
        for (PathIterator it = r.getPathIterator(null); !it.isDone(); it.next()) {
            int t = it.currentSegment(c);
            int k = t == PathIterator.SEG_QUADTO ? 2 : t == PathIterator.SEG_CUBICTO ? 3 : t == PathIterator.SEG_CLOSE ? 0 : 1;
            String[] pt = new String[k];
            boolean still = k > 0;
            for (int i = 0; i < k; i++) { pt[i] = n(c[2 * i]) + "," + n(c[2 * i + 1]); still &= pt[i].equals(at); }
            if (t == PathIterator.SEG_MOVETO) { sub = new StringBuilder("M" + pt[0]); drawn = 0; at = pt[0]; continue; }
            if (t == PathIterator.SEG_CLOSE) { if (drawn > 1) d.append(sub).append("Z"); continue; }
            if (still) continue;
            sub.append(k == 1 ? "L" : k == 2 ? "Q" : "C").append(String.join(" ", pt));
            at = pt[k - 1];
            drawn++;
        }
        System.out.println(d);
    }
}
