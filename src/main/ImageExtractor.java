import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class ImageExtractor extends PDFStreamEngine {

    static int dpi = 300;
    static int POINTS_IN_INCH = 72;

    public static void main(String[] args) throws IOException {
        String outPath = "";
        for (int i = 1; i < args.length; i+=2) {
            String key = args[i];
            String val = args[i+1];
            if (key.equals("-dpi")) dpi = Integer.parseInt(val);
            if (key.equals("-outpath")) outPath = val;
        }

        Path path = Paths.get(args[0]);
        if (Files.isDirectory(path)) {
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".pdf")) {
                        try {
                            processFile(file);
                        }
                        catch (Exception e) { }
                    }
                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(path, visitor);
        } else {
            processFile(path);
        }
    }

    static void processFile(Path path) throws IOException {
        PDDocument doc = PDDocument.load(path.toFile());
        try {
            PDFRenderer renderer = new PDFRenderer(doc);
            int count = 1;
            for (int pageIndex = 0; pageIndex < doc.getNumberOfPages(); pageIndex++) {
                ImageExtractor ext = new ImageExtractor();
                ext.processPage(doc.getPage(pageIndex));
                for (ImagePosition image : ext.buffer) {
                    Rectangle2D region = new Rectangle2D.Float(image.x, image.y, image.w, image.h);
                    RenderedImage renderedImage = ext.renderRect(renderer, pageIndex, region);
                    String fileName = path.getFileName() + "_" + String.valueOf(count) + ".png";
                    ImageIO.write(renderedImage, "png", new File(fileName));
                    System.out.println(fileName + " is saved.");
                    count++;
                }
            }
        } finally {
            doc.close();
        }
    }

    List<ImagePosition> buffer = new ArrayList<>();

    public ImageExtractor() throws IOException {
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if("Do".equals(operation)) {
            COSName objectName = (COSName)operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject)xobject;
                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
                PDRectangle pageRect = this.getCurrentPage().getCropBox();
                float w = ctmNew.getScalingFactorX();
                float h = ctmNew.getScalingFactorY();
                float x = ctmNew.getTranslateX();
                float y = pageRect.getHeight() - ctmNew.getTranslateY() - h;
                buffer.add(new ImagePosition(x, y, w, h));
            }
            else if(xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject)xobject;
                showForm(form);
            }
        }
        else {
            super.processOperator(operator, operands);
        }
    }

    private RenderedImage renderRect(PDFRenderer renderer, int pageIndex, Rectangle2D rect) throws IOException {
        BufferedImage image = createImage(rect);
        Graphics2D graphics = createGraphics(image, rect);
        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }

    private BufferedImage createImage(Rectangle2D rect) {
        double scale = dpi / POINTS_IN_INCH;
        double bitmapWidth  = rect.getWidth()  * scale;
        double bitmapHeight = rect.getHeight() * scale;
        return new BufferedImage((int)bitmapWidth, (int)bitmapHeight, BufferedImage.TYPE_INT_RGB);
    }

    private Graphics2D createGraphics(BufferedImage image, Rectangle2D rect) {
        double scale = dpi / POINTS_IN_INCH;
        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.getX(), -rect.getY()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);
        return graphics;
    }
}