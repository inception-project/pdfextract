# pdfextract
PDF extractor using PDFBox.  
The jar file can be found at [releases](https://github.com/paperai/pdfextract/releases).
* [PDFExtract.jl](https://github.com/hshindo/PDFExtract.jl): julia wrapper for pdfextract

## PDFExtractor
Extract texts and draws from PDF.
```
java -classpath pdfextract.jar paperai.pdfextract.PDFExtractor [file or directory] [-glyph]
```

<p align="center"><img src="https://github.com/paperai/pdfextract/blob/master/PDFExtractor.png" width="1200"></p>

In the figure, blue square indicates font coordinates, and red square indicates glyph coordinates.

### Output Format
Each line is either `Text` or `Draw` as follows.

#### Text
1. Page number
1. Character or `NO_UNICODE` (when unicode mapping is unavailable)
1. Font coordinate (x, y, width, height)
1. [optional] Glyph coordinate (x, y, width, height)

For font coordinate or glyph coordinate, `_` indicates the previous

#### Draw
1. Page number
1. Draw operation, either one of
    * `STROKE_PATH`
    * `FILL_PATH`
    * `FILL_STROKE_PATH`
    * `CURVE_TO`
    * `LINE_TO`
    * `MOVE_TO`
    * `RECTANGLE`
1. Coordinate

### Output Example
```
1	P	106.4301	754.63226	5.478471	10.705882
1	r	111.90857	754.63226	3.4879298	10.705882
1	o	114.99301	754.63226	4.4832	10.705882
1	c	119.47621	754.63226	3.981082	10.705882
1	e	123.45729	754.63226	3.981082	10.705882
1	e	127.43837	754.63226	3.981082	10.705882
1	d	131.41945	754.63226	4.4832	10.705882
1	i	135.90265	754.63226	2.4926593	10.705882
1	n	138.39531	754.63226	4.4832	10.705882
1	g	142.87851	754.63226	4.4832	10.705882
1	s	147.36171	754.63226	3.4879298	10.705882
1	o	153.09125	754.63226	4.4832	10.705882
1	f	157.57445	754.63226	2.4926593	10.705882
1	t	162.30872	754.63226	2.4926593	10.705882
1	h	164.80138	754.63226	4.4832	10.705882
...
...
4	[MOVE_TO]	323.779	200.93103
4	[LINE_TO]	509.279	200.93103
4	[LINE_TO]	509.279	62.675964
4	[LINE_TO]	323.779	62.675964
4	[RECTANGLE]	323.779	200.93103	509.279	200.93103	509.279	62.676025	323.779	62.676025
4	[FILL_PATH]
```

## ImageExtractor
Extract images from PDF as PNG format.
```
java -classpath pdfextract.jar paperai.pdfextract.ImageExtractor <file or directory> -dpi <dpi> -o <output directory>
```

For example,
```
java -classpath pdfextract.jar ImageExtractor xxx.pdf -dpi 300 -o /work
```
