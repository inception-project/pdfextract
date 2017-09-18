# pdfextract
PDF extractor using PDFBox.
Download: [pdfextract.jar](https://cl.naist.jp/~shindo/pdfextract.jar)

## PDFExtractor
Extract text, draw and image from PDF.
```
java -classpath pdfextract.jar PDFExtractor <pdf filename or directory name> <options...>
```

### Options
* -text: extracts texts
  * -bounding: extracts bounding coordinates
  * -glyph: extracts glyph coordinates
* -fontName: extracts fontName
* -draw: extracts draws
* -image: extracts images

For example,
```
java -classpath pdfextract.jar PDFExtractor xxx.pdf -text -bounding
```
extracts only texts with bounding coordinates from `xxx.pdf`.

<p align="center"><img src="https://github.com/paperai/pdfextract/blob/master/PDFExtractor.png" width="1200"></p>

In the figure, blue square indicates bounding coordinates, and red square indicates glyph coordinates.

### Output Format
Each line is either one of "TEXT", "DRAW", "IMAGE", or empty.

#### Text
1. Page number
1. "TEXT"
1. Character
1. [Optional] bounding x coordinate
1. [Optional] bounding y coordinate
1. [Optional] bounding width
1. [Optional] bounding height
1. [Optional] glyph x coordinate
1. [Optional] glyph y coordinate
1. [Optional] glyph width
1. [Optional] glyph height
1. [Optional] Font name

#### Draw
1. Page number
1. "DRAW"
1. Operation ("LINE_TO", "CURVE_TO", etc.)

#### Image
1. Page number
1. "IMAGE"
1. x coordinate
1. y coordinate
1. width
1. height

### Example
```
1	TEXT	P	106.4301	754.63226	5.478471	10.705882	106.4301	757.06213	5.424672	5.8550596	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	r	111.90857	754.63226	3.4879298	10.705882	112.31206	758.963	3.290669	3.9541826	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	o	114.99301	754.63226	4.4832	10.705882	115.23511	758.963	3.9541826	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	c	119.47621	754.63226	3.981082	10.705882	119.7452	758.963	3.5417283	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	e	123.45729	754.63226	3.981082	10.705882	123.73525	758.963	3.4161987	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	e	127.43837	754.63226	3.981082	10.705882	127.71633	758.963	3.4161987	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	d	131.41945	754.63226	4.4832	10.705882	131.55394	756.79315	4.590797	6.240615	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	i	135.90265	754.63226	2.4926593	10.705882	136.342	757.05316	1.9277761	5.9626565	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	n	138.39531	754.63226	4.4832	10.705882	138.52084	758.963	4.124544	4.03488	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	g	142.87851	754.63226	4.4832	10.705882	142.95024	758.963	4.16041	5.801261	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	s	147.36171	754.63226	3.4879298	10.705882	147.50517	758.95404	3.13824	4.0797124	LMQTGC+NimbusRomNo9L-ReguItal

1	TEXT	o	153.09125	754.63226	4.4832	10.705882	153.33334	758.963	3.9541826	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	f	157.57445	754.63226	2.4926593	10.705882	156.2564	756.83795	5.119815	7.9352646	LMQTGC+NimbusRomNo9L-ReguItal

1	TEXT	t	162.30872	754.63226	2.4926593	10.705882	162.64047	758.02155	2.3222978	4.994285	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	h	164.80138	754.63226	4.4832	10.705882	164.97174	756.79315	4.1155777	6.204749	LMQTGC+NimbusRomNo9L-ReguItal
1	TEXT	e	169.28458	754.63226	3.981082	10.705882	169.56253	758.963	3.4161987	4.052813	LMQTGC+NimbusRomNo9L-ReguItal
```
