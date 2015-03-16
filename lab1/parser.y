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
    preamble document               { debug ("latex"); }
    | preamble document NEWLINES    { debug ("latex"); }
    ;

preamble:
    DOCCLASS LBRACE normal_t RBRACE header_list                                { debug("preamble"); } 
    | DOCCLASS LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE header_list   { debug("preamble"); } 
    ;

header_list:
     header_list USEPKG LBRACE normal_t RBRACE                                          { debug("header_list"); } 
    | header_list USEPKG LBRACKET normal_t RBRACKET LBRACE normal_t RBRACE              { debug("header_list"); } 
    | header_list TITLE                                                                 { debug("header_list"); } 
    | header_list AUTHOR                                                                { debug("header_list"); } 
    | header_list NEWLINES                                                              { debug("header_list"); }
    |                                                                                   { debug("header_list"); } 
    ;

document:
    BEGIN_DOC body END_DOC     { debug("document"); } 
    | BEGIN_DOC END_DOC        { debug("document"); } 
    ;

body:
    body text          { debug("body"); } 
    | body command     { debug("body"); } 
    | text             { debug("body"); } 
    | command          { debug("body"); } 
    ;

command:
    MAKETITLE                          { debug("command"); } 
    | BEGIN_ITEM item_list END_ITEM    { debug("command"); } 
    | INGRAPH LBRACE normal_t RBRACE   { debug("command"); } 
    | CITE LBRACE normal_t RBRACE      { debug("command"); } 
    | BEGIN_BIB bib_list END_BIB       { debug("command"); } 
    ;

item_list:                      
    ITEM text                  { debug("item_list"); } 
    | item_list ITEM text      { debug("item_list"); } 
    ;

bib_list:
    BBITEM LBRACE normal_t RBRACE normal_t             { debug("bib_list"); } 
    | bib_list BBITEM LBRACE normal_t RBRACE normal_t  { debug("bib_list"); } 

text:
    normal_t       { debug("text"); } 
    | italic_t     { debug("text"); } 
    | bold_t       { debug("text"); } 
    | NEWLINES     { debug("text"); }
    ;

normal_t:
    STRING         { debug("normal_t"); } 
    | CHAR         { debug("normal_t"); } 
    ;

bold_t:
    bold_exp                                            { debug("bold_t"); }
    | TXTIT LBRACE bold_exp RBRACE                      { debug("bold_t"); }   
    | TXTIT LBRACE CHAR bold_exp RBRACE                 { debug("bold_t"); } 
    | TXTIT LBRACE STRING bold_exp RBRACE               { debug("bold_t"); } 
    | TXTIT LBRACE bold_exp CHAR RBRACE                 { debug("bold_t"); } 
    | TXTIT LBRACE CHAR bold_exp CHAR RBRACE            { debug("bold_t"); } 
    | TXTIT LBRACE STRING bold_exp CHAR RBRACE          { debug("bold_t"); } 
    | TXTIT LBRACE bold_exp STRING RBRACE               { debug("bold_t"); } 
    | TXTIT LBRACE CHAR bold_exp STRING RBRACE          { debug("bold_t"); } 
    | TXTIT LBRACE STRING bold_exp STRING RBRACE        { debug("bold_t"); } 
    ;

bold_exp:
    TXTBF LBRACE STRING RBRACE                          { debug("bold_exp"); }  
    | TXTBF LBRACE CHAR RBRACE                          { debug("bold_exp"); }
    ;

italic_t:
    italic_exp                                          { debug("italic_t"); }
    | TXTBF LBRACE italic_exp RBRACE                    { debug("italic_t"); }
    | TXTBF LBRACE CHAR italic_exp RBRACE               { debug("italic_t"); }
    | TXTBF LBRACE STRING italic_exp RBRACE             { debug("italic_t"); }
    | TXTBF LBRACE italic_exp CHAR RBRACE               { debug("italic_t"); }
    | TXTBF LBRACE CHAR italic_exp CHAR RBRACE          { debug("italic_t"); }
    | TXTBF LBRACE STRING italic_exp CHAR RBRACE        { debug("italic_t"); }
    | TXTBF LBRACE italic_exp STRING RBRACE             { debug("italic_t"); }
    | TXTBF LBRACE CHAR italic_exp STRING RBRACE        { debug("italic_t"); }
    | TXTBF LBRACE STRING italic_exp STRING RBRACE      { debug("italic_t"); }
    ;


italic_exp:
    TXTIT LBRACE STRING RBRACE { debug("italic_exp"); }
    | TXTIT LBRACE CHAR RBRACE { debug("italic_exp"); }
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
    htmlGEN_init(2); 
   
    yyparse();

    htmlGEN_print_all();
    htmlGEN_free();
    return 0;
}

