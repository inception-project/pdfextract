package paperai.pdfextract;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class RegionExtractor {

    static int POINTS_IN_INCH = 72;

    PDFRenderer renderer;
    int dpi;

    public RegionExtractor(PDDocument doc, int dpi) {
        this.renderer = new PDFRenderer(doc);
        this.dpi = dpi;
    }

    RenderedImage extract(int pageIndex, Float x, Float y, Float w, Float h) throws IOException {
        Rectangle2D rect = new Rectangle2D.Float(x, y, w, h);
        BufferedImage image = createImage(rect);
        Graphics2D graphics = createGraphics(image, rect);
        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }

    BufferedImage createImage(Rectangle2D rect) {
        double scale = dpi / POINTS_IN_INCH;
        double bitmapWidth  = rect.getWidth()  * scale;
        double bitmapHeight = rect.getHeight() * scale;
        return new BufferedImage((int)bitmapWidth, (int)bitmapHeight, BufferedImage.TYPE_INT_RGB);
    }

    Graphics2D createGraphics(BufferedImage image, Rectangle2D rect) {
        double scale = dpi / POINTS_IN_INCH;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.getX(), -rect.getY()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);
        return graphics;
    }
}