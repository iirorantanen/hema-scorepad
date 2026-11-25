import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** The "compare" verb of emulator.sh: prints "identical", or the box in which two screenshots differ. */
class Compare {
    // The status bar above this line holds a clock, which changes between two screenshots.
    static final int TOP = 150;

    public static void main(String[] args) throws Exception {
        BufferedImage a = ImageIO.read(new File(args[0])), b = ImageIO.read(new File(args[1]));
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) {
            fail("sizes differ: %dx%d and %dx%d", a.getWidth(), a.getHeight(), b.getWidth(), b.getHeight());
        }
        int x1 = a.getWidth(), y1 = a.getHeight(), x2 = 0, y2 = 0;
        for (int y = TOP; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) {
                    x1 = Math.min(x1, x);
                    y1 = Math.min(y1, y);
                    x2 = Math.max(x2, x + 1);
                    y2 = Math.max(y2, y + 1);
                }
            }
        }
        if (x2 > 0) fail("differs in [%d,%d][%d,%d]", x1, y1, x2, y2);
        System.out.println("identical");
    }

    static void fail(String format, Object... values) {
        System.out.println(String.format(format, values));
        System.exit(1);
    }
}
