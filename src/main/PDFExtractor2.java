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
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

import java.awt.geom.Point2D;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class PDFExtractor2 extends PDFTextStripper {

    public static void main(String[] args) throws IOException {
        String path = args[0];
        Path p = Paths.get(path);
        PDDocument doc = PDDocument.load(p.toFile());
        //try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8"))) {
        try (Writer w = new BufferedWriter(new OutputStreamWriter(System.out, "UTF-8"))) {
            for (int i = 0; i < 1; i++) {
                PDFExtractor2 ext = new PDFExtractor2(i, w);
                //ext.processPage(doc.getPage(i));
                ext.writeText(doc, w);
                for (int j = 0; j < 100; j++) {
                    ext.write(ext.buffer.get(j));
                }
                //ext.write();
            }
        }
    }

    Writer output;
    int pageIndex;
    List<TextPosition> buffer = new ArrayList<>();

    public PDFExtractor2(int pageIndex, Writer output) throws IOException {
        super();
        this.output = output;
        this.pageIndex = pageIndex;
    }

    void writeLine(Object... values) throws IOException {
        List<String> l = new ArrayList<>();
        for (Object o : values) l.add(String.valueOf(o));
        output.write(String.join("\t", l));
        output.write("\n");
    }

    void write(TextPosition text) throws IOException {
        writeLine(text.getUnicode(), text.getFontSize(), text.getX(), text.getWidth(), text.getHeight(), text.getFontSize());
    }

    @Override
    public void processTextPosition(TextPosition text) {
        //super.processTextPosition(text);
        buffer.add(text);
    }
}
