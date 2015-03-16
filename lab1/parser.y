%{
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include "log.h"
#include "htmlGEN.h"

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


%token LATEX_NEWLINE
%token NEWLINES

%token DOLLAR
%token LBRACE
%token RBRACE
%token LBRACKET
%token RBRACKET

%start latex

%error-verbose
 
%%

latex:
    preamble document               { debug ("Parser: latex"); }
    | preamble document NEWLINES    { debug ("Parser: latex"); }
    ;

preamble:
    DOCCLASS LBRACE normal_t RBRACE header_list                                { debug("Parser: preamble"); } 
    | DOCCLASS LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE header_list   { debug("Parser: preamble"); } 
    ;

header_list:
     header_list USEPKG LBRACE normal_t RBRACE                                          { debug("Parser: header_list"); } 
    | header_list USEPKG LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE              { debug("Parser: header_list"); } 
    | header_list TITLE                                                                 { debug("Parser: header_list"); } 
    | header_list AUTHOR                                                                { debug("Parser: header_list"); } 
    | header_list NEWLINES                                                              { debug("Parser: header_list"); }
    |                                                                                   { debug("Parser: header_list"); } 
    ;

document:
    BEGIN_DOC body END_DOC     { debug("Parser: document"); } 
    | BEGIN_DOC END_DOC        { debug("Parser: document"); } 
    ;

body:
    body text          { debug("Parser: body"); } 
    | body command     { debug("Parser: body"); } 
    | text             { debug("Parser: body"); } 
    | command          { debug("Parser: body"); } 
    ;

command:
    MAKETITLE                          { debug("Parser: command"); } 
    | BEGIN_ITEM item_list END_ITEM    { debug("Parser: command"); } 
    | INGRAPH LBRACE normal_t RBRACE   { debug("Parser: command"); } 
    | CITE LBRACE normal_t RBRACE      { debug("Parser: command"); } 
    | BEGIN_BIB bib_list END_BIB       { debug("Parser: command"); } 
    ;

item_list:                      
    ITEM text                  { debug("Parser: item_list"); } 
    | item_list ITEM text      { debug("Parser: item_list"); } 
    ;

bib_list:
    BBITEM LBRACE normal_t RBRACE normal_t             { debug("Parser: bib_list"); } 
    | bib_list BBITEM LBRACE normal_t RBRACE normal_t  { debug("Parser: bib_list"); } 

text:
    normal_t       { debug("Parser: text"); } 
    | italic_t     { debug("Parser: text"); } 
    | bold_t       { debug("Parser: text"); } 
    | NEWLINES     { debug("Parser: text"); }
    ;

normal_t:
    STRING         { debug("Parser: normal_t"); } 
    | CHAR         { debug("Parser: normal_t"); } 
    ;

bold_t:
    bold_exp                                            { debug("Parser: bold_t"); }
    | TXTIT LBRACE bold_exp RBRACE                      { debug("Parser: bold_t"); }   
    | TXTIT LBRACE CHAR bold_exp RBRACE                 { debug("Parser: bold_t"); } 
    | TXTIT LBRACE STRING bold_exp RBRACE               { debug("Parser: bold_t"); } 
    | TXTIT LBRACE bold_exp CHAR RBRACE                 { debug("Parser: bold_t"); } 
    | TXTIT LBRACE CHAR bold_exp CHAR RBRACE            { debug("Parser: bold_t"); } 
    | TXTIT LBRACE STRING bold_exp CHAR RBRACE          { debug("Parser: bold_t"); } 
    | TXTIT LBRACE bold_exp STRING RBRACE               { debug("Parser: bold_t"); } 
    | TXTIT LBRACE CHAR bold_exp STRING RBRACE          { debug("Parser: bold_t"); } 
    | TXTIT LBRACE STRING bold_exp STRING RBRACE        { debug("Parser: bold_t"); } 
    ;

bold_exp:
    TXTBF LBRACE STRING RBRACE                          { debug("Parser: bold_exp"); }  
    | TXTBF LBRACE CHAR RBRACE                          { debug("Parser: bold_exp"); }
    ;

italic_t:
    italic_exp                                          { debug("Parser: italic_t"); }
    | TXTBF LBRACE italic_exp RBRACE                    { debug("Parser: italic_t"); }
    | TXTBF LBRACE CHAR italic_exp RBRACE               { debug("Parser: italic_t"); }
    | TXTBF LBRACE STRING italic_exp RBRACE             { debug("Parser: italic_t"); }
    | TXTBF LBRACE italic_exp CHAR RBRACE               { debug("Parser: italic_t"); }
    | TXTBF LBRACE CHAR italic_exp CHAR RBRACE          { debug("Parser: italic_t"); }
    | TXTBF LBRACE STRING italic_exp CHAR RBRACE        { debug("Parser: italic_t"); }
    | TXTBF LBRACE italic_exp STRING RBRACE             { debug("Parser: italic_t"); }
    | TXTBF LBRACE CHAR italic_exp STRING RBRACE        { debug("Parser: italic_t"); }
    | TXTBF LBRACE STRING italic_exp STRING RBRACE      { debug("Parser: italic_t"); }
    ;


italic_exp:
    TXTIT LBRACE STRING RBRACE { debug("Parser: italic_exp"); }
    | TXTIT LBRACE CHAR RBRACE { debug("Parser: italic_exp"); }
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
    info("Latex-HTML start.");
    htmlGEN_init(2); 
   
    yyparse();

    htmlGEN_print_all();
    htmlGEN_free();
    info("Latex-HTML end.");
    return 0;
}

