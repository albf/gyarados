%{
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include "log.h"
#include "htmlGEN.h"

char *concat(int count, ...);

// Indicate if it is paragrah start.
int is_p_start;

// Avoid cycle
int is_bold;
int is_italic;

// Iteminize
int list_level;
int item_level;

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

%type <str> normal_t bold_text bold_t italic_text italic_t

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
    | header_list TITLE  LBRACE normal_t RBRACE                                         { debug("Parser: header_list"); } 
    | header_list AUTHOR LBRACE normal_t RBRACE                                         { debug("Parser: header_list"); } 
    | header_list NEWLINES                                                              { debug("Parser: header_list"); }
    |                                                                                   { debug("Parser: header_list"); } 
    ;

document:
    BEGIN_DOC body END_DOC     { debug("Parser: document"); } 
    | BEGIN_DOC END_DOC        { debug("Parser: document"); } 
    ;

body:
    body text           { debug("Parser: body"); } 
    | body command      { debug("Parser: body"); } 
	| body math 		{ debug("Parser: body"); }
    | text              { debug("Parser: body"); } 
    | command           { debug("Parser: body"); } 
	| math 				{ debug("Parser: body"); }
    ;

math:
	DOLLAR math_exp DOLLAR { debug("Parser: math"); }

math_exp:
	| math_exp STRING 		{ debug("Parser: math_exp"); }
	| math_exp CHAR 		{ debug("Parser: math_exp"); }
	| math_exp LBRACE 		{ debug("Parser: math_exp"); }
	| math_exp RBRACE 		{ debug("Parser: math_exp"); }
	| STRING 				{ debug("Parser: math_exp"); }
	| CHAR 					{ debug("Parser: math_exp"); }
	| LBRACE 				{ debug("Parser: math_exp"); }
	| RBRACE 				{ debug("Parser: math_exp"); }
	;

command:
    MAKETITLE                          { debug("Parser: command"); } 
    | BEGIN_ITEM item_list END_ITEM    { debug("Parser: command"); } 
    | INGRAPH LBRACE normal_t RBRACE   { debug("Parser: command"); 
                                         if(access ($3, F_OK) != -1) {
                                             if((htmlGEN_add_string(concat(5, htmlGEN_image_html_start, $3, 
                                                htmlGEN_image_html_middle, $3, htmlGEN_image_html_end), 0, 0, 0, 0)<0) ||
                                                 (htmlGEN_add_string(" ", 0, 0, 0, 0)<0)) {
                                                return -1;
                                             }
                                         }
                                         else {
                                             error("Coulnd't find file: %s", $3);
                                             return -1;
                                         }
                                       } 
    | CITE LBRACE normal_t RBRACE      { debug("Parser: command"); 
                                         if(is_p_start==1) {
                                           is_p_start = 0;
                                             if(htmlGEN_add_string(concat(3, htmlGEN_ref_symbol_start, $3, htmlGEN_ref_symbol_end), 0, 0 ,1 ,0)<0) {
                                                return -1;
                                             }
                                         }
                                         else {
                                             if(htmlGEN_add_string(concat(3, htmlGEN_ref_symbol_start, $3, htmlGEN_ref_symbol_end), 0, 0 ,0 ,0)<0) {
                                                return -1;
                                             }
                                         }
                                       } 
    | start_bib bib_list END_BIB       { debug("Parser: command"); } 
    ;

start_bib:
    BEGIN_BIB                           { debug("Parser: start_bib"); 
                                          if(htmlGEN_create_bib(2)<0) {
                                            return -1;
                                          }
                                        }

bib_list:
    BBITEM LBRACE normal_t RBRACE normal_t             { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($3, $5)<0) {
                                                            return -1;
                                                         }
                                                       } 
    | BBITEM LBRACE normal_t RBRACE italic_t           { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($3, concat(3, htmlGEN_italic_html_start, $5, htmlGEN_italic_html_end))<0) {
                                                            return -1;
                                                         }
                                                       } 
    | BBITEM LBRACE normal_t RBRACE bold_t             { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($3, concat(3, htmlGEN_bold_html_start, $5, htmlGEN_bold_html_end))<0) {
                                                            return -1;
                                                         }
                                                       } 
    | bib_list BBITEM LBRACE normal_t RBRACE normal_t  { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($4, $6)<0) {
                                                            return -1;
                                                         }
                                                       } 
    | bib_list BBITEM LBRACE normal_t RBRACE italic_t  { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($4, concat(3, htmlGEN_italic_html_start, $6, htmlGEN_italic_html_end))<0) {
                                                            return -1;
                                                         }
                                                       } 
    | bib_list BBITEM LBRACE normal_t RBRACE bold_t    { debug("Parser: bib_list"); 
                                                         if(htmlGEN_add_ref($4, concat(3, htmlGEN_bold_html_start, $6, htmlGEN_bold_html_end))<0) {
                                                            return -1;
                                                         }
                                                       } 


item_list:                      
    ITEM text                  { debug("Parser: item_list"); } 
    | item_list ITEM text      { debug("Parser: item_list"); } 
    ;

text:
    normal_t       { debug("Parser: text"); 
                     if(is_p_start==1) {
                         debug("Parser: Start <P>");
                         if(htmlGEN_add_string($1, 0, 0, 1, 0)<0) {
                            return -1;
                         }
                         is_p_start=0;
                     }
                     else {
                         if(htmlGEN_add_string(concat(2, " ", $1), 0, 0, 0, 0)<0) {
                            return -1;
                         }
                     }

                   } 
    | italic_t     { debug("Parser: text"); 
                     if(is_p_start==1) {
                         debug("Parser: Start <P>");
                         if(htmlGEN_add_string($1, 0, 1, 1, 0)<0) {
                            return -1;
                         }
                         is_p_start=0;
                     }
                     else {
                         if(htmlGEN_add_string(concat(2, " ", $1), 0, 1, 0, 0)<0) {
                            return -1;
                         }
                     }
                     is_italic = 0;
                     is_bold = 0;
                   } 
    | bold_t       { debug("Parser: text"); 
                     if(is_p_start==1) {
                         debug("Parser: Start <P>");
                         if(htmlGEN_add_string($1, 1, 0, 1, 0)<0) {
                            return -1;
                         }
                         is_p_start=0;
                     }
                     else {
                         if(htmlGEN_add_string(concat(2, " ", $1), 1, 0, 0, 0)<0) {
                            return -1;
                         }
                     }
                     is_italic=0;
                     is_bold = 0;
                    } 

    | NEWLINES     { debug("Parser: text"); 
                     if(is_p_start==0) {
                         is_p_start=1;
                         htmlGEN_add_string("", 0, 0, 0, 1);
                         htmlGEN_add_string("\n", 0, 0, 0, 0);
                     }
                   }
    ;

normal_t:
    STRING         { debug("Parser: normal_t"); 
                     $$ = $1;
                   } 
    | CHAR         { debug("Parser: normal_t"); 
                     $$ = $1; 
                   } 
    ;

bold_text:
    STRING                     { debug("Parser: bold_text"); 
                                 $$ = $1;
                               } 
    | CHAR                     { debug("Parser: bold_text"); 
                                 $$ = $1;
                               } 
    | italic_t                 { debug("Parser: bold_text"); 
                                 $$ = concat(3, htmlGEN_italic_html_start, $1, htmlGEN_italic_html_start); 
                               } 
    | bold_text STRING         { debug("Parser: bold_text"); 
                                 $$ = concat(3, $1," ", $2);
                               } 
    | bold_text CHAR           { debug("Parser: bold_text"); 
                                 $$ = concat(3, $1," ", $2);
                               } 
    | bold_text italic_t       { debug("Parser: bold_text"); 
                                 $$ = concat(5, $1," ", htmlGEN_italic_html_start, $2, htmlGEN_italic_html_start); 
                               } 
    ;

bold_t:
    TXTBF LBRACE bold_text RBRACE                          { debug("Parser: bold_t");
                                                             if(is_bold == 1) {
                                                                error("Trying to insert bold twice. Not allowed here.");
                                                                return -1;
                                                             }
                                                             is_bold = 1;                                
                                                             $$ = $3;
                                                           }  
    ;

italic_text:
    STRING                     { debug("Parser: italic_text"); 
                                 $$ = $1;
                               }     
    | CHAR                     { debug("Parser: italic_text"); 
                                 $$ = $1;
                               }     
    | bold_t                   { debug("Parser: italic_text"); 
                                 $$ = concat(3, htmlGEN_bold_html_start, $1, htmlGEN_bold_html_start); 
                               }
    | italic_text STRING       { debug("Parser: italic_text"); 
                                 $$ = concat(3, $1," ", $2);
                               }
    | italic_text CHAR         { debug("Parser: italic_text"); 
                                 $$ = concat(3, $1," ", $2);
                               }
    | italic_text bold_t       { debug("Parser: italic_text"); 
                                 $$ = concat(5, $1," ", htmlGEN_bold_html_start, $2, htmlGEN_bold_html_start); 
                               }

italic_t:
    TXTIT LBRACE italic_text RBRACE                        { debug("Parser: italic_t"); 
                                                             if(is_italic == 1) {
                                                                error("Trying to insert italic twice. Not allowed here.");
                                                                return -1;
                                                             }
                                                             is_italic = 1;                                
                                                             $$ = $3;
                                                           }
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
    int is_error;

    info("Latex-HTML start.");
    htmlGEN_init(2); 
    
    // Define flag to start paragraph at first chance.
    is_p_start = 1;   

    // Avoid cycles.
    is_bold = 0;
    is_italic = 0;

    // Control of itemize.
    list_level = 0;
    item_level = 0;

    is_error = yyparse();

    if(is_error >= 0) {
        htmlGEN_print_all();
    }
    htmlGEN_free();
    info("Latex-HTML end.");
    return 0;
}

