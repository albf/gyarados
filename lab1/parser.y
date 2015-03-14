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
%token T_BEGIN
%token T_END
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
    DOCCLASS header_list
    ;

header_list:
    header_list USEPKG
    | header_list TITLE
    | header_list AUTHOR
    |
    ;

document:
    T_BEGIN LBRACE key_wrd RBRACE body T_END LBRACE key_wrd RBRACE
    ;

key_wrd:
    STRING
    ;

body:
    body command
    | body text
    |
    ;

command:
    MAKETITLE
    | itemize
    | INGRAPH LBRACE key_wrd RBRACE
    | CITE LBRACE NUM RBRACE
    | key_wrd
    ;

itemize:
    T_BEGIN LBRACE key_wrd RBRACE item_list T_END LBRACE key_wrd RBRACE
    ;

item_list:
    ITEM text
    | item_list ITEM text
    ;

text:
    normal_t
    | italic_t
    | bold_t
    |
    ;

normal_t:
    STRING
    | CHAR
    | normal_t CHAR
    | normal_t STRING
    ;

bold_t:
    TXTBF LBRACE normal_t RBRACE
    ;

italic_t:
    TXTIT LBRACE normal_t RBRACE
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

