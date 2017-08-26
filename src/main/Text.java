import org.apache.pdfbox.pdmodel.font.PDFont;

public class Text {
    String unicode;
    PDFont font;
    float x;
    float y;
    float w;
    float h;
    float bx;
    float by;
    float bw;
    float bh;

    public Text(String unicode, PDFont font, float x, float y, float w, float h, float bx, float by, float bw, float bh) {
        this.unicode = unicode;
        this.font = font;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.bx = bx;
        this.by = by;
        this.bw = bw;
        this.bh = bh;
        /*
        this.unicode = p.getUnicode();
        this.font = p.getFont();
        PDFontDescriptor fontDesc = font.getFontDescriptor();

        this.x = p.getX();
        this.y = p.getY();
        this.w = font.getStringWidth(unicode) * fontSize / 100.0F;
        this.w = p.getWidth();
        this.h = (fontDesc.getAscent() - fontDesc.getDescent()) * fontSize / 1000.0F;
        this.h = p.getHeightDir();
        this.y -= fontDesc.getDescent() / 100.0F * fontSize;
        this.y -= h;
        this.h = fontDesc.getFontBoundingBox().getHeight() / 1000.0F * fontSize;
        this.p = p;
        */
    }
}