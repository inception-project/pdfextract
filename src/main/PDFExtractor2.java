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
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class PDFExtractor2 extends PDFGraphicsStreamEngine {

    static boolean hasText = false;
    static boolean hasDraw = false;
    static boolean hasImage = false;

    public static void main(String[] args) throws IOException {
        String path = "";
        for (String arg : args) {
            if (arg.equals("-text")) hasText = true;
            else if (arg.equals("-draw")) hasDraw = true;
            else if (arg.equals("-image")) hasImage = true;
            else path = arg;
        }

        Path p = Paths.get(path);
        if (Files.isDirectory(p)) {
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".pdf")) processFile(file);
                    return FileVisitResult.CONTINUE;
                }
            };
            Files.walkFileTree(p, visitor);
        }
        else processFile(p);
    }

    static void processFile(Path path) throws IOException {
        PDDocument doc = PDDocument.load(path.toFile());
        //try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8"))) {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"))) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                PDFExtractor ext = new PDFExtractor(doc.getPage(i), i, w);
                ext.processPage(doc.getPage(i));
                ext.write();
                w.write("\n");
            }
        }
    }

    Writer output;
    int pageIndex;
    int pageRotation;
    PDRectangle pageSize;
    Matrix translateMatrix;
    final GlyphList glyphList;
    float currentX;
    float currentY;
    List<String> imageBuffer;
    List<Object> buffer = new ArrayList<>();

    public PDFExtractor2(PDPage page, int pageIndex, Writer output) throws IOException {
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

        ImageExtractor ext = new ImageExtractor();
        ext.processPage(page);
        imageBuffer = ext.buffer;
    }

    float getPageHeight() { return getPage().getCropBox().getHeight(); }

    void addText(TextPosition p) {
        if (hasText) buffer.add(p);
    }
    void addDraw(String op, Object... values) {
        if (hasDraw) buffer.add(new DrawPosition(op, values));
    }

    void writeLine(Object... values) throws IOException {
        List<String> l = new ArrayList<>();
        for (Object o : values) l.add(String.valueOf(o));
        output.write(String.join("\t", l));
        output.write("\n");
    }

    void writeText(List<TextPosition> textBuffer) throws IOException {
        for (TextPosition p : textBuffer) {
            PDFont font = p.getFont();
            PDFontDescriptor fontDesc = font.getFontDescriptor();
            float descent = fontDesc.getDescent() / 1000F;
            writeLine("TEXT", pageIndex+1, p.getUnicode(), p.getXDirAdj(), p.getYDirAdj(), p.getWidthDirAdj(), p.getHeightDir(),
                    descent, font.getName(), p.getFontSize(), p.getWidthOfSpace());
        }
        textBuffer.clear();
    }

    void write() throws IOException {
        List<TextPosition> textBuffer = new ArrayList<>();
        float averageW = 0;
        String prevType = null;

        for (Object obj : buffer) {
            String type = "";
            if (obj instanceof TextPosition) type = "TEXT";
            else if (obj instanceof DrawPosition) type = "DRAW";
            assert !type.equals("");

            if (prevType != null && !type.equals(prevType)) writeLine("");
            prevType = type;

            if (type.equals("TEXT") && hasText) {
                TextPosition p = (TextPosition)obj;
                if (textBuffer.isEmpty()) {
                    averageW = p.getWidth();
                }
                else {
                    TextPosition prev = textBuffer.get(textBuffer.size() - 1);
                    float expectedX = prev.getX() + prev.getWidth() + averageW * 0.3f;
                    float y1 = prev.getY();
                    float h1 = prev.getHeight();
                    float y2 = p.getY();
                    float h2 = p.getHeight();
                    boolean overlapped = (y2 < y1+0.1f && y2 > y1-0.1f) || (y2 <= y1 && y2 >= y1-h1) || (y1 <= y2 && y1 >= y2-h2);
                    if (p.getX() > expectedX || !overlapped) {
                        writeText(textBuffer);
                        writeLine("");
                        averageW = 0;
                    }
                    else averageW = (averageW + p.getWidth()) / 2;
                }
                textBuffer.add(p);
            }
            else if (type.equals("DRAW")) {
                DrawPosition p = (DrawPosition)obj;
                writeLine(type, pageIndex+1);
            }
        }
        if (!textBuffer.isEmpty()) writeText(textBuffer);
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        String s = imageBuffer.get(0);
        imageBuffer.remove(0);
        writeLine("IMAGE", pageIndex+1, s);
    }

    @Override
    public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3) throws IOException {
        addDraw("RECTANGLE", p0.getX(), p0.getY(), p1.getX(), p1.getY(), p2.getX(), p2.getY(), p3.getX(), p3.getY());
    }

    @Override
    public void clip(int i) throws IOException { }

    @Override
    public void moveTo(float x, float y) throws IOException {
        currentX = x;
        currentY = y;
    }

    @Override
    public void lineTo(float x, float y) throws IOException {
        float x1 = x;
        float y1 = getPageHeight() - y;
        float x2 = currentX;
        float y2 = getPageHeight() - currentY;

        if (x1 > x2 || (x1 == x2 && y1 > y2)) addDraw("LINE", x2, y2, x1-x2, y1-y2);
        else addDraw("LINE", x1, y1, x2-x1, y2-y1);

        currentX = x;
        currentY = y;
    }

    @Override
    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) throws IOException {
        addDraw("CURVE_TO", x1, y1, x2, y2, x3, y3);
    }

    @Override
    public Point2D getCurrentPoint() throws IOException { return new Point2D.Float(currentX, currentY); }

    @Override
    public void closePath() throws IOException { }

    @Override
    public void endPath() throws IOException { }

    @Override
    public void strokePath() throws IOException { addDraw("STROKE_PATH"); }

    @Override
    public void fillPath(int i) throws IOException { addDraw("FILL_PATH"); }

    @Override
    public void fillAndStrokePath(int i) throws IOException { addDraw("FILL_AND_STROKE_PATH"); }

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
        addText(p);
    }

    public class DrawPosition {
        String op;
        Object[] values;

        public DrawPosition(String op, Object[] values) {
            this.op = op;
            this.values = values;
        }
    }

    public class ImageExtractor extends PDFStreamEngine {

        List<String> buffer = new ArrayList<>();

        public ImageExtractor() throws IOException {
            addOperator(new Concatenate());
            addOperator(new DrawObject());
            addOperator(new SetGraphicsStateParameters());
            addOperator(new Save());
            addOperator(new Restore());
            addOperator(new SetMatrix());
        }

        void addLine(Object... values) throws IOException {
            List<String> l = new ArrayList<>();
            for (Object o : values) l.add(String.valueOf(o));
            buffer.add(String.join("\t", l));
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
                    addLine(x, y, w, h);
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
