%{
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

/*#define MAX 1000

char table[MAX][MAX][MAX];

char *concat(int count, ...);
void selectPrime(char *);
void selectField(char *, char *);
void sucess(int *, int);*/

%}
 
%union{
    char *str;
    int  intval;
}

%token <str> STRING
%token <intval> NUM
%token <str> CHAR


%token BEGIN_DOC
%token END_DOC
%token BEGIN_ITEM
%token END_ITEM
%token BEGIN_BIB
%token END_BIB


%token DOCCLASS
%token USEPKG
%token TITLE
%token AUTHOR
%token MAKETITLE
%token TXTBF
%token TXTIT
%token INGRAPH
%token CITE;
%token BBITEM
%token ITEM


%token NEWLINE
%token DOLLAR
%token LBRACE
%token RBRACE
%token LBRACKET
%token RBRACKET

%start latex

%error-verbose
 
%%

latex:
    preamble document
    ;

preamble:
    DOCCLASS LBRACE normal_t RBRACE header_list
    | DOCCLASS LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE header_list
    ;

header_list:
    header_list USEPKG LBRACE normal_t RBRACE
    | header_list USEPKG LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE
    | header_list TITLE
    | header_list AUTHOR
    |
    ;

document:
    BEGIN_DOC body END_DOC
    | BEGIN_DOC END_DOC
    ;

body:
    body text
    | body command
    | text
    | command
    ;

command:
    MAKETITLE
    | BEGIN_ITEM item_list END_ITEM
    | INGRAPH LBRACE normal_t RBRACE
    | CITE LBRACE normal_t RBRACE
    | BEGIN_BIB bib_list END_BIB
    ;

item_list:
    ITEM text
    | item_list ITEM text
    ;

bib_list:
    BBITEM LBRACE normal_t RBRACE normal_t
    | bib_list BBITEM LBRACE normal_t RBRACE normal_t

text:
    normal_t 
    | italic_t
    | bold_t 
    ;

normal_t:
    STRING
    | CHAR
    ;

bold_t:
    TXTBF LBRACE STRING RBRACE
    | TXTBF LBRACE CHAR RBRACE
    ;

italic_t:
    TXTIT LBRACE STRING RBRACE
    | TXTIT LBRACE CHAR RBRACE
    ;

    
%%
 
char* concat(int count, ...)
{
    va_list ap;
    int len = 1, i;

    va_start(ap, count);
    for(i=0 ; i<count ; i++)
        len += strlen(va_arg(ap, char*));
    va_end(ap);

    char *result = (char*) calloc(sizeof(char),len);
    int pos = 0;

    // Actually concatenate strings
    va_start(ap, count);
    for(i=0 ; i<count ; i++)
    {
        char *s = va_arg(ap, char*);
        strcpy(result+pos, s);
        pos += strlen(s);
    }
    va_end(ap);

    return result;
}


int yyerror(const char* errmsg)
{
    printf("\n*** Erro: %s\n", errmsg);
}
 
int yywrap(void) { return 1; }
 
int main(int argc, char** argv)
{
     yyparse();
     return 0;
}

