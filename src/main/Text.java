import org.apache.pdfbox.pdmodel.font.PDFont;

public class Text {
    String unicode;
    PDFont font;
    float bx;
    float by;
    float bw;
    float bh;
    float gx;
    float gy;
    float gw;
    float gh;

    public Text(String unicode, PDFont font, float bx, float by, float bw, float bh, float gx, float gy, float gw, float gh) {
        this.unicode = unicode;
        this.font = font;
        this.bx = bx;
        this.by = by;
        this.bw = bw;
        this.bh = bh;
        this.gx = gx;
        this.gy = gy;
        this.gw = gw;
        this.gh = gh;
    }
}