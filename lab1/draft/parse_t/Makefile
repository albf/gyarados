

CFLAGS=-g
BISON=bison
FLEX=flex
 
parser: clean parser.o scanner.o
	$(CC) -o parser scanner.o parser.o -g
 
parser.c: parser.y
	$(BISON) -d parser.y  -o parser.c -v
 
scanner.c: scanner.l
	$(FLEX)  -o scanner.c scanner.l

clean:
	rm -f scanner.c scanner.o parser.c parser.o parser.h parser parser.output out

run: parser
	./parser < text 2> out
