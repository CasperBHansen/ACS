all:
	pdflatex report.tex

clean:
	rm -rf *.{aux,log,out,toc}

package:
	@pdflatex report.tex
	@mkdir ../pkg
	@zip -r ../pkg/src.zip ../src
	@cp report.pdf ../pkg

.PHONY: clean
