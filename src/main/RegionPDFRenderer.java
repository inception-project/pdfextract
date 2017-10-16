import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RegionPDFRenderer {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get(args[0]);
        PDDocument doc = PDDocument.load(path.toFile());
        int pageIndex = 0;

        Rectangle2D region = new Rectangle2D.Float(226.0f, 149.0f, 4.0f, 6.0f);

        try {
            RegionPDFRenderer renderer = new RegionPDFRenderer(doc, 500);
            RenderedImage image = renderer.renderRect(pageIndex, region);
            ImageIO.write(image, "png", new File("image.png"));
        } finally {
            doc.close();
        }
    }

    private static final int POINTS_IN_INCH = 72;

    private final PDFRenderer renderer;
    private final int resolutionDotPerInch;

    public RegionPDFRenderer(PDDocument doc, int resolutionDotPerInch) {
        this.renderer = new PDFRenderer(doc);
        this.resolutionDotPerInch = resolutionDotPerInch;
    }

    RenderedImage renderRect(int pageIndex, Rectangle2D rect) throws IOException {
        BufferedImage image = createImage(rect);
        Graphics2D graphics = createGraphics(image, rect);
        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }

    private BufferedImage createImage(Rectangle2D rect) {
        double scale = resolutionDotPerInch / POINTS_IN_INCH;
        double bitmapWidth  = rect.getWidth()  * scale;
        double bitmapHeight = rect.getHeight() * scale;
        return new BufferedImage((int)bitmapWidth, (int)bitmapHeight, BufferedImage.TYPE_INT_RGB);
    }

    private Graphics2D createGraphics(BufferedImage image, Rectangle2D rect) {
        double scale = resolutionDotPerInch / POINTS_IN_INCH;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.getX(), -rect.getY()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);
        return graphics;
    }
}