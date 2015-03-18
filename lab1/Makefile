CFLAGS=-g
BISON=bison
FLEX=flex
 
all: parser

parser: parser.o scanner.o log.o htmlGEN.o
	$(CC) -o parser scanner.o parser.o log.o htmlGEN.o -g
	rm parser.c parser.h parser.o parser.output scanner.c scanner.o
 
parser.c: parser.y
	$(BISON) -d parser.y  -o parser.c -v

scanner.c: scanner.l
	$(FLEX)  -o scanner.c scanner.l

log.o: log.c
	$(CC) -g -c log.c

htmlGEN.o: htmlGEN.c
	$(CC) -g -c htmlGEN.c

clean:
	rm -f scanner.c scanner.o parser.c parser.o parser.h parser parser.output log.o htmlGEN.o
