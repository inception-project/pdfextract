import javafx.beans.binding.ObjectExpression;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.*;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.pagenavigation.PDThreadBead;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class PDFExtractor extends PDFGraphicsStreamEngine {

    public static void main(String[] args) throws IOException {
        String path = args[0];
        Path p = Paths.get(path);
        PDDocument doc = PDDocument.load(p.toFile());
        //try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8"))) {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"))) {
            for (int i = 5; i < 6; i++) {
                PDFExtractor ext = new PDFExtractor(doc.getPage(i), i+1, w);
                ext.processPage(doc.getPage(i));
                ext.write();
            }
        }
    }

    Writer output;
    int pageIndex;
    int pageRotation;
    PDRectangle pageSize;
    Matrix translateMatrix;
    final GlyphList glyphList;
    List<Image> imageBuffer;
    List<Object> buffer = new ArrayList<>();

    AffineTransform flipAT;
    AffineTransform rotateAT;
    AffineTransform transAT;

    public PDFExtractor(PDPage page, int pageIndex, Writer output) throws IOException {
        super(page);
        this.pageIndex = pageIndex;
        this.output = output;

        String path = "org/apache/pdfbox/resources/glyphlist/additional.txt";
        InputStream input = GlyphList.class.getClassLoader().getResourceAsStream(path);
        this.glyphList = new GlyphList(GlyphList.getAdobeGlyphList(), input);

        this.pageRotation = page.getRotation();
        this.pageSize = page.getCropBox();
        if (this.pageSize.getLowerLeftX() == 0.0F && this.pageSize.getLowerLeftY() == 0.0F) {
            this.translateMatrix = null;
        } else {
            this.translateMatrix = Matrix.getTranslateInstance(-this.pageSize.getLowerLeftX(), -this.pageSize.getLowerLeftY());
        }

        // taken from DrawPrintTextLocations for setting flipAT, rotateAT and transAT
        PDRectangle cropBox = page.getCropBox();
        // flip y-axis
        flipAT = new AffineTransform();
        flipAT.translate(0, page.getBBox().getHeight());
        flipAT.scale(1, -1);

        // page may be rotated
        rotateAT = new AffineTransform();
        int rotation = page.getRotation();
        if (rotation != 0) {
            PDRectangle mediaBox = page.getMediaBox();
            switch (rotation) {
                case 90:
                    rotateAT.translate(mediaBox.getHeight(), 0);
                    break;
                case 270:
                    rotateAT.translate(0, mediaBox.getWidth());
                    break;
                case 180:
                    rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
                    break;
                default:
                    break;
            }
            rotateAT.rotate(Math.toRadians(rotation));
        }
        // cropbox
        transAT = AffineTransform.getTranslateInstance(-cropBox.getLowerLeftX(), cropBox.getLowerLeftY());

        ImageExtractor ext = new ImageExtractor();
        ext.processPage(page);
        imageBuffer = ext.buffer;
    }

    float getPageHeight() { return getPage().getCropBox().getHeight(); }

    void addDraw(String op, float... values) {
        buffer.add(new Draw(op, values));
    }

    void writeLine(Object... values) throws IOException {
        List<String> l = new ArrayList<>();
        for (Object o : values) l.add(String.valueOf(o));
        output.write(String.join("\t", l));
        output.write("\n");
    }

    void writeText(List<Text> textBuffer) throws IOException {
        List<String> l = new ArrayList<>();
        for (Text t : textBuffer) l.add(t.unicode);
        String unicode = String.join("", l);

        l.clear();
        for (Text t : textBuffer) l.add(String.valueOf(t.x));
        String x = String.join(" ", l);

        l.clear();
        for (Text t : textBuffer) l.add(String.valueOf(t.w));
        String w = String.join(" ", l);

        Text t0 = textBuffer.get(0);
        writeLine(pageIndex, "TEXT", unicode, x, t0.y, w, t0.h);
    }

    void writeText2(List<Text> textBuffer) throws IOException {
        for (Text t : textBuffer) {
            writeLine(pageIndex, "TEXT", t.unicode, t.bx, t.by, t.bw, t.bh, t.x, t.y, t.w, t.h);
        }
        output.write("\n");
    }

    void writeDraw(List<Draw> drawBuffer) throws IOException {
        for (Draw d : drawBuffer) {
            writeLine(pageIndex, "DRAW", d.op);
        }
        output.write("\n");
    }

    void write() throws IOException {
        int i = 0;
        while (i < buffer.size()) {
            Object obj = buffer.get(i);
            if (obj instanceof Text) {
                Text prev = (Text)obj;
                float totalW = 0;
                List<Text> textBuffer = new ArrayList<>();
                while (i < buffer.size()) {
                    obj = buffer.get(i);
                    if (obj instanceof Text == false) break;
                    Text curr = (Text)obj;
                    if (textBuffer.isEmpty()) textBuffer.add(curr);
                    else {
                        float expectedX = prev.bx + prev.bw + totalW / textBuffer.size() * 0.3f;
                        if (curr.by == prev.by && curr.bh == prev.bh && curr.bx <= expectedX) textBuffer.add(curr);
                        else break;
                    }
                    totalW += curr.bw;
                    prev = curr;
                    i++;
                }
                writeText(textBuffer);
            }
            else if (obj instanceof Draw) {
                List<Draw> drawBuffer = new ArrayList<>();
                while (i < buffer.size()) {
                    obj = buffer.get(i);
                    if (obj instanceof Draw == false) break;
                    Draw d = (Draw)obj;
                    drawBuffer.add(d);
                    i++;
                    if (d.op.endsWith("_PATH")) {
                        writeDraw(drawBuffer);
                        break;
                    }
                }
            }
            else if (obj instanceof Image) {
                Image image = (Image)obj;
                writeLine(pageIndex, "IMAGE", image.x, image.y, image.w, image.h);
                i++;
            }
            else i++;
        }
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        Image i = imageBuffer.get(0);
        buffer.add(i);
        imageBuffer.remove(0);
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        addDraw("RECTANGLE", (float)p0.getX(), (float)p0.getY(), (float)p1.getX(), (float)p1.getY(),
                (float)p2.getX(), (float)p2.getY(), (float)p3.getX(), (float)p3.getY());
    }

    @Override
    public void clip(int i) throws IOException { }

    @Override
    public void moveTo(float x, float y) throws IOException {
        addDraw("MOVE_TO", x, getPageHeight() - y);
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        addDraw("LINE_TO", x, getPageHeight() - y);
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        float h = getPageHeight();
        addDraw("CURVE_TO", x1, h - y1, x2, h - y2, x3, h - y3);
    }

    @Override
    public Point2D getCurrentPoint() throws IOException { return new Point2D.Float(0.0f, 0.0f); }

    @Override
    public void closePath() throws IOException { }

    @Override
    public void endPath() throws IOException { }

    @Override
    public void strokePath() throws IOException { addDraw("STROKE_PATH"); }

    @Override
    public void fillPath(int i) throws IOException { addDraw("FILL_PATH"); }

    @Override
    public void fillAndStrokePath(int i) throws IOException { addDraw("FILL_STROKE_PATH"); }

    @Override
    public void shadingFill(COSName cosName) throws IOException { }

    @Override
    public void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {
        // taken from LegacyPDFStreamEngine.showGlyph
        PDGraphicsState state = this.getGraphicsState();
        Matrix ctm = state.getCurrentTransformationMatrix();
        float fontSize = state.getTextState().getFontSize();
        float horizontalScaling = state.getTextState().getHorizontalScaling() / 100.0F;
        Matrix textMatrix = this.getTextMatrix();
        BoundingBox bbox = font.getBoundingBox();
        if (bbox.getLowerLeftY() < -32768.0F) {
            bbox.setLowerLeftY(-(bbox.getLowerLeftY() + 65536.0F));
        }

        float glyphHeight = bbox.getHeight() / 2.0F;
        PDFontDescriptor fontDescriptor = font.getFontDescriptor();
        float height;
        if (fontDescriptor != null) {
            height = fontDescriptor.getCapHeight();
            if (height != 0.0F && (height < glyphHeight || glyphHeight == 0.0F)) {
                glyphHeight = height;
            }
        }

        height = glyphHeight / 1000.0F;

        float displacementX = displacement.getX();
        if (font.isVertical()) {
            displacementX = font.getWidth(code) / 1000.0F;
            TrueTypeFont ttf = null;
            if (font instanceof PDTrueTypeFont) {
                ttf = ((PDTrueTypeFont)font).getTrueTypeFont();
            } else if (font instanceof PDType0Font) {
                PDCIDFont cidFont = ((PDType0Font)font).getDescendantFont();
                if(cidFont instanceof PDCIDFontType2) {
                    ttf = ((PDCIDFontType2)cidFont).getTrueTypeFont();
                }
            }

            if (ttf != null && ttf.getUnitsPerEm() != 1000) {
                displacementX *= 1000.0F / (float)ttf.getUnitsPerEm();
            }
        }

        float tx = displacementX * fontSize * horizontalScaling;
        float ty = displacement.getY() * fontSize;
        Matrix td = Matrix.getTranslateInstance(tx, ty);
        Matrix nextTextRenderingMatrix = td.multiply(textMatrix).multiply(ctm);
        float nextX = nextTextRenderingMatrix.getTranslateX();
        float nextY = nextTextRenderingMatrix.getTranslateY();
        float dxDisplay = nextX - textRenderingMatrix.getTranslateX();
        float dyDisplay = height * textRenderingMatrix.getScalingFactorY();
        float glyphSpaceToTextSpaceFactor = 0.001F;
        if (font instanceof PDType3Font) {
            glyphSpaceToTextSpaceFactor = font.getFontMatrix().getScaleX();
        }

        float spaceWidthText = 0.0F;
        try {
            spaceWidthText = font.getSpaceWidth() * glyphSpaceToTextSpaceFactor;
        } catch (Throwable e) {
            //LOG.warn(e, e);
        }

        if (spaceWidthText == 0.0F) {
            spaceWidthText = font.getAverageFontWidth() * glyphSpaceToTextSpaceFactor;
            spaceWidthText *= 0.8F;
        }
        if (spaceWidthText == 0.0F) spaceWidthText = 1.0F;

        float spaceWidthDisplay = spaceWidthText * textRenderingMatrix.getScalingFactorX();
        unicode = font.toUnicode(code, this.glyphList);
        if (unicode == null) {
            if (font instanceof PDSimpleFont) {
                char c = (char)code;
                unicode = new String(new char[]{c});
            }
            else unicode = "[NO_UNICODE]";
        }
        unicode = unicode.trim();
        if (unicode.isEmpty()) return;

        Matrix translatedTextRenderingMatrix;
        if (this.translateMatrix == null) {
            translatedTextRenderingMatrix = textRenderingMatrix;
        } else {
            translatedTextRenderingMatrix = Matrix.concatenate(this.translateMatrix, textRenderingMatrix);
            nextX -= this.pageSize.getLowerLeftX();
            nextY -= this.pageSize.getLowerLeftY();
        }

        TextPosition text = new TextPosition(pageRotation, pageSize.getWidth(), pageSize.getHeight(), translatedTextRenderingMatrix,
                nextX, nextY, Math.abs(dyDisplay), dxDisplay, Math.abs(spaceWidthDisplay),
                unicode, new int[]{code}, font, fontSize, (int)(fontSize * textMatrix.getScalingFactorX()));

        Shape shape1 = calculateGlyphBounds(textRenderingMatrix, font, code);
        Rectangle2D.Double r1 = (Rectangle2D.Double)shape1.getBounds2D();
        Shape shape2 = calculateGlyphBoundsFromText(text);
        Rectangle2D.Double r2 = (Rectangle2D.Double)shape2.getBounds2D();
        Text t = new Text(unicode, font, (float)r1.x, (float)r1.y, (float)r1.width, (float)r1.height,
                (float)r2.x, (float)r2.y, (float)r2.width, (float)r2.height);
        buffer.add(t);
    }

    // taken from writeString in DrawPrintTextLocations
    Shape calculateGlyphBoundsFromText(TextPosition text) throws IOException {
        // glyph space -> user space
        // note: text.getTextMatrix() is *not* the Text Matrix, it's the Text Rendering Matrix
        AffineTransform at = text.getTextMatrix().createAffineTransform();

        // show rectangle with the real vertical bounds, based on the font bounding box y values
        // usually, the height is identical to what you see when marking text in Adobe Reader
        PDFont font = text.getFont();
        BoundingBox bbox = font.getBoundingBox();

        // advance width, bbox height (glyph space)
        float xadvance = font.getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
        Rectangle2D.Float rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance, bbox.getHeight());

        if (font instanceof PDType3Font) {
            // bbox and font matrix are unscaled
            at.concatenate(font.getFontMatrix().createAffineTransform());
        }
        else {
            // bbox and font matrix are already scaled to 1000
            at.scale(1/1000f, 1/1000f);
        }
        Shape s = at.createTransformedShape(rect);
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);
        return s;
    }

    // taken from DrawPrintTextLocations.java
    // this calculates the real (except for type 3 fonts) individual glyph bounds
    Shape calculateGlyphBounds(Matrix textRenderingMatrix, PDFont font, int code) throws IOException {
        GeneralPath path = null;
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());
        if (font instanceof PDType3Font) {
            // It is difficult to calculate the real individual glyph bounds for type 3 fonts
            // because these are not vector fonts, the content stream could contain almost anything
            // that is found in page content streams.
            PDType3Font t3Font = (PDType3Font) font;
            PDType3CharProc charProc = t3Font.getCharProc(code);
            if (charProc != null) {
                BoundingBox fontBBox = t3Font.getBoundingBox();
                PDRectangle glyphBBox = charProc.getGlyphBBox();
                if (glyphBBox != null) {
                    // PDFBOX-3850: glyph bbox could be larger than the font bbox
                    glyphBBox.setLowerLeftX(Math.max(fontBBox.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                    glyphBBox.setLowerLeftY(Math.max(fontBBox.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                    glyphBBox.setUpperRightX(Math.min(fontBBox.getUpperRightX(), glyphBBox.getUpperRightX()));
                    glyphBBox.setUpperRightY(Math.min(fontBBox.getUpperRightY(), glyphBBox.getUpperRightY()));
                    path = glyphBBox.toGeneralPath();
                }
            }
        }
        else if (font instanceof PDVectorFont) {
            PDVectorFont vectorFont = (PDVectorFont) font;
            path = vectorFont.getPath(code);

            if (font instanceof PDTrueTypeFont) {
                PDTrueTypeFont ttFont = (PDTrueTypeFont) font;
                int unitsPerEm = ttFont.getTrueTypeFont().getHeader().getUnitsPerEm();
                at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
            }
            if (font instanceof PDType0Font) {
                PDType0Font t0font = (PDType0Font) font;
                if (t0font.getDescendantFont() instanceof PDCIDFontType2) {
                    int unitsPerEm = ((PDCIDFontType2) t0font.getDescendantFont()).getTrueTypeFont().getHeader().getUnitsPerEm();
                    at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
                }
            }
        }
        else if (font instanceof PDSimpleFont) {
            PDSimpleFont simpleFont = (PDSimpleFont) font;

            // these two lines do not always work, e.g. for the TT fonts in file 032431.pdf
            // which is why PDVectorFont is tried first.
            String name = simpleFont.getEncoding().getName(code);
            path = simpleFont.getPath(name);
        }
        else {
            // shouldn't happen, please open issue in JIRA
            System.out.println("Unknown font class: " + font.getClass());
        }
        if (path == null) return null;
        Shape s = at.createTransformedShape(path.getBounds2D());
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);
        s = transAT.createTransformedShape(s);
        return s;
    }

    public class ImageExtractor extends PDFStreamEngine {

        List<Image> buffer = new ArrayList<>();

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
                    buffer.add(new Image(x, y, w, h));
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
    }
}
