# pdfextract
PDF extractor using PDFBox.  
The jar file can be found at [releases](https://github.com/paperai/pdfextract/releases).
* [PDFExtract.jl](https://github.com/hshindo/PDFExtract.jl): julia wrapper for pdfextract

## PDFExtractor
Extract texts, draws and images from PDF.
```
java -classpath pdfextract.jar PDFExtractor <file or directory>
```

<p align="center"><img src="https://github.com/paperai/pdfextract/blob/master/PDFExtractor.png" width="1200"></p>

In the figure, blue square indicates font coordinates, and red square indicates glyph coordinates.

### Output Format
Each line is `Text` or `Draw`.

#### Text
1. ID
1. Page number
1. "TEXT"
1. Character
1. Font coordinate (x, y, width, height)
1. Glyph coordinate (x, y, width, height)

#### Draw
1. ID
1. Page number
1. "DRAW"
1. Operation ("LINE_TO", "CURVE_TO", etc.)
1. Coordinate

### Output Example
```
18914	7	TEXT	p	108.535255 489.41638 5.400005 13.014011	108.58926 494.42758 5.022004 7.3116064	JSOXBN+NimbusRomNo9L-Regu
18915	7	TEXT	e	113.935265 489.41638 4.795204 13.014011	114.20526 494.42758 4.3092036 5.0760045	JSOXBN+NimbusRomNo9L-Regu
18916	7	TEXT	r	118.73047 489.41638 3.5964031 13.014011	118.78447 494.42758 3.564003 4.968004	JSOXBN+NimbusRomNo9L-Regu
18917	7	TEXT	f	122.326866 489.41638 3.5964031 13.014011	122.54287 492.0192 3.9204035 7.376406	JSOXBN+NimbusRomNo9L-Regu
18918	7	TEXT	o	125.92327 489.41638 5.400005 13.014011	126.23647 494.42758 4.762804 5.0760045	JSOXBN+NimbusRomNo9L-Regu
18919	7	TEXT	r	131.32327 489.41638 3.5964031 13.014011	131.37727 494.42758 3.564003 4.968004	JSOXBN+NimbusRomNo9L-Regu
18920	7	TEXT	m	134.91968 489.41638 8.402408 13.014011	135.09248 494.42758 8.197207 4.968004	JSOXBN+NimbusRomNo9L-Regu
18921	7	TEXT	a	143.32208 489.41638 4.795204 13.014011	143.72168 494.42758 4.374004 5.0760045	JSOXBN+NimbusRomNo9L-Regu
18922	7	TEXT	n	148.1173 489.41638 5.400005 13.014011	148.2901 494.42758 5.065204 4.968004	JSOXBN+NimbusRomNo9L-Regu
18923	7	TEXT	c	153.51729 489.41638 4.795204 13.014011	153.7873 494.42758 4.1796036 5.0760045	JSOXBN+NimbusRomNo9L-Regu
18924	7	TEXT	e	158.31248 489.41638 4.795204 13.014011	158.58249 494.42758 4.3092036 5.0760045	JSOXBN+NimbusRomNo9L-Regu

18925	7	TEXT	o	169.7605 489.41638 5.400005 13.014011	170.0737 494.42758 4.762804 5.0760045	JSOXBN+NimbusRomNo9L-Regu
18926	7	TEXT	f	175.16049 489.41638 3.5964031 13.014011	175.3765 492.0192 3.9204035 7.376406	JSOXBN+NimbusRomNo9L-Regu

18927	7	TEXT	t	185.42052 489.41638 3.0024025 13.014011	185.56091 493.1424 2.8728025 6.3612056	JSOXBN+NimbusRomNo9L-Regu
18928	7	TEXT	h	188.42291 489.41638 5.400005 13.014011	188.52011 492.0192 5.1624045 7.376406	JSOXBN+NimbusRomNo9L-Regu
18929	7	TEXT	e	193.8229 489.41638 4.795204 13.014011	194.09291 494.42758 4.3092036 5.0760045	JSOXBN+NimbusRomNo9L-Regu
...

9409	4	DRAW	MOVE_TO	129.75435	61.265076
9410	4	DRAW	LINE_TO	482.24384	61.265076
9411	4	DRAW	STROKE_PATH
```

## ImageExtractor
Extract images from PDF as PNG format.
```
java -classpath pdfextract.jar ImageExtractor <file or directory> -dpi <dpi> -o <output directory>
```

For example,
```
java -classpath pdfextract.jar ImageExtractor xxx.pdf -dpi 300 -o /work
```
