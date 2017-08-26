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
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TextExtractor extends PDFGraphicsStreamEngine {

    public static void main(String[] args) throws IOException {
        String path = args[0];
        Path p = Paths.get(path);
        PDDocument doc = PDDocument.load(p.toFile());
        //try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8"))) {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"))) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                TextExtractor ext = new TextExtractor(doc.getPage(i), i, w);
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
    List<Text> texts = new ArrayList<>();

    public TextExtractor(PDPage page, int pageIndex, Writer output) throws IOException {
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
    }

    void writeLine(Object... values) throws IOException {
        List<String> l = new ArrayList<>();
        for (Object o : values) l.add(String.valueOf(o));
        output.write(String.join("\t", l));
        output.write("\n");
    }

    void writeText(List<Text> buffer) throws IOException {
        List<String> l = new ArrayList<>();
        for (Text t : buffer) l.add(t.unicode);
        String unicode = String.join("", l);

        l.clear();
        for (Text t : buffer) l.add(String.valueOf(t.x));
        String x = String.join(" ", l);

        l.clear();
        for (Text t : buffer) l.add(String.valueOf(t.w));
        String w = String.join(" ", l);

        try {
            Text t0 = buffer.get(0);
            writeLine(pageIndex+1, unicode, x, t0.y, w, t0.h);
        }
        catch (Exception e) { }
    }

    void write() throws IOException {
        List<Text> buffer = new ArrayList<>();
        float averageW = 0;

        for (Text text : texts) {
            if (buffer.isEmpty()) averageW = text.w;
            else {
                Text prev = buffer.get(buffer.size() - 1);
                float expectedX = prev.x + prev.w + averageW * 0.3f;
                float y1 = prev.y;
                float h1 = prev.h;
                float y2 = text.y;
                float h2 = text.h;
                boolean overlapped = (y2 < y1+0.1f && y2 > y1-0.1f) || (y2 <= y1 && y2 >= y1-h1) || (y1 <= y2 && y1 >= y2-h2);
                boolean consistent = y1 == y2 && h1 == h2 && prev.font.getName() == text.font.getName();
                if (text.x > expectedX || !overlapped || !consistent) {
                    writeText(buffer);
                    buffer.clear();
                    averageW = 0;
                }
                else averageW = (averageW + text.w) / 2;
            }
            buffer.add(text);
        }
        if (!buffer.isEmpty()) writeText(buffer);
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException { }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException { }

    @Override
    public void clip(int i) throws IOException { }

    @Override
    public void moveTo(float x, float y) throws IOException { }

    @Override
    public void lineTo(float x, float y) throws IOException { }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException { }

    @Override
    public Point2D getCurrentPoint() throws IOException { return new Point2D.Float(0, 0); }

    @Override
    public void closePath() throws IOException { }

    @Override
    public void endPath() throws IOException { }

    @Override
    public void strokePath() throws IOException { }

    @Override
    public void fillPath(int i) throws IOException { }

    @Override
    public void fillAndStrokePath(int i) throws IOException { }

    @Override
    public void shadingFill(COSName cosName) throws IOException { }

    @Override
    public void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code, String unicode, Vector displacement) throws IOException {
        // from LegacyPDFStreamEngine.showGlyph
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
            else unicode = "NO_UNICODE";
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

        TextPosition p = new TextPosition(pageRotation, pageSize.getWidth(), pageSize.getHeight(), translatedTextRenderingMatrix,
                nextX, nextY, Math.abs(dyDisplay), dxDisplay, Math.abs(spaceWidthDisplay),
                unicode, new int[]{code}, font, fontSize, (int)(fontSize * textMatrix.getScalingFactorX()));
        //try {
        //    texts.add(new Text(p));
        //}
        //catch (Exception e) { }
    }
}
