%{
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include "log.h"
#include "htmlGEN.h"

char *concat(int count, ...);
int set_list_init(int value, int level);
int get_list_init(int level);
int init_list_init(int size);
void free_list_init();

// Indicate if it is paragrah start.
int is_p_start;
int avoid_p;

// Avoid cycle
int is_bold;
int is_italic;

// Iteminize
int list_level;
int * is_list_init;
int is_list_init_size;

// Indicate error of sintax
int is_yyerror;

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

%type <str> normal_t bold_text bold_t italic_text italic_t special_symbol math_exp math header_text

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
     header_list USEPKG LBRACE header_text RBRACE                                              { debug("Parser: header_list"); } 
    | header_list USEPKG LBRACKET header_text RBRACKET LBRACE normal_t RBRACE                  { debug("Parser: header_list"); } 
    | header_list TITLE  LBRACE header_text RBRACE                                             { debug("Parser: header_list"); 
                                                                                                 htmlGEN_set_title($4);
                                                                                               } 
    | header_list AUTHOR LBRACE header_text RBRACE                                             { debug("Parser: header_list"); } 
    | header_list NEWLINES                                                                     { debug("Parser: header_list"); }
    |                                                                                          { debug("Parser: header_list"); } 
    ;

document:
    BEGIN_DOC body END_DOC     { debug("Parser: document"); } 
    | BEGIN_DOC END_DOC        { debug("Parser: document"); 
                                 if(is_p_start==0) {
                                    htmlGEN_add_string("", 0, 0, 0, 1);
                                 }
                               } 
    ;

body:
    body text           { debug("Parser: body"); } 
    | body command      { debug("Parser: body"); } 
    | text              { debug("Parser: body"); } 
    | command           { debug("Parser: body"); } 
    ;

command:
    INGRAPH LBRACE normal_t RBRACE   { debug("Parser: command"); 
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
    | start_bib bib_list END_BIB       { debug("Parser: command"); } 
    ;

start_bib:
    BEGIN_BIB                           { debug("Parser: start_bib"); 
                                          if(htmlGEN_create_bib(2)<0) {
                                            return -1;
                                          }
                                        }

bib_list:
    BBITEM LBRACE normal_t RBRACE header_text             { debug("Parser: bib_list"); 
                                                            if(htmlGEN_add_ref($3, $5)<0) {
                                                                return -1;
                                                            }
                                                          } 
    | bib_list BBITEM LBRACE normal_t RBRACE header_text  { debug("Parser: bib_list"); 
                                                            if(htmlGEN_add_ref($4, $6)<0) {
                                                                return -1;
                                                            }
                                                          } 
    | NEWLINES                                            { debug("Parser: bib_list"); }
    | bib_list NEWLINES                                   { debug("Parser: bib_list"); }


special_symbol:
    BEGIN_ITEM          { debug("Parser: special_symbol");
                          list_level++;
                          $$ = concat(1, htmlGEN_list_start);
                          avoid_p = 1;
                        }
    | END_ITEM          { debug("Parser: special_symbol");
                          set_list_init(0, list_level);
                          list_level--;
                          if(list_level < 0) {
                            error("Closing list that doesn't exist.");
                            return -1;
                          }
                          avoid_p = 1;
                          $$ = concat(1, htmlGEN_list_end);
                        }
    | ITEM              { debug("Parser: special_symbol");
                            if(list_level <= 0) {
                                error("Adding item in a non-list. Declare the list first.");
                                return -1;
                            } 
                            // Verify if the list is already started.
                            if(get_list_init(list_level) == 1) {
                              $$ = concat(3, htmlGEN_item_end, "\n", htmlGEN_item_start);
                            }
                            else {
                              // if not, just add the \n and the item start.
                              set_list_init(1, list_level);
                              $$ = concat(2, "\n", htmlGEN_item_start);
                            }
                            avoid_p = 1;
                        }
    | MAKETITLE         { debug("Parser: special_symbol"); 
                          char * title = htmlGEN_get_title();
                          if(title == NULL) {
                            error("Use of \\maketitle without \\title at the header.");
                            return -1;
                          }
                          $$ = concat(3, htmlGEN_title_start, title, htmlGEN_title_end); 
                        }
    | CITE LBRACE normal_t RBRACE      { debug("Parser: special_symbol"); 
                                             $$ = concat(3, htmlGEN_ref_symbol_start, $3, htmlGEN_ref_symbol_end);
                                       }


header_text:
    normal_t                { debug("Parser: header_text");
                              $$ = $1;
                              is_italic = 0;
                              is_bold = 0;
                            }
    | italic_t              { debug("Parser: header_text");
                              is_italic = 0;
                              is_bold = 0;
                              $$ = $1;
                            }
    | bold_t                { debug("Parser: header_text");
                              is_italic = 0;
                              is_bold = 0;
                              $$ = $1;
                            }
    | header_text normal_t  { debug("Parser: header_text");
                              $$ = concat(3, $1," ", $2);
                              is_italic = 0;
                              is_bold = 0;
                            }
    | header_text bold_t    { debug("Parser: header_text");
                              $$ = concat(3, $1," ", $2);
                              is_italic = 0;
                              is_bold = 0;
                            }
    | header_text italic_t  { debug("Parser: header_text");
                              $$ = concat(3, $1," ", $2);
                              is_italic = 0;
                              is_bold = 0;
                            }

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
                     is_italic = 0;
                     is_bold = 0;
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
                     if((is_p_start==0)&&(avoid_p==0)) {
                         is_p_start=1;
                         htmlGEN_add_string("", 0, 0, 0, 1);
                         htmlGEN_add_string("\n", 0, 0, 0, 0);
                     }
                     if(avoid_p == 1) {
                        avoid_p = 0;
                     }
                   }
    | math         { debug("Parser: text");
                      if(is_p_start==1) {
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
    ;

math:
	DOLLAR math_exp DOLLAR { debug("Parser: math"); 
                                htmlGEN_include_math();
                                $$ = concat(3, "$ ", $2, " $");
                           }

math_exp:
	 math_exp STRING 		{ debug("Parser: math_exp"); 
                                $$ = concat(3, $1," ", $2);
                            }
	| math_exp CHAR 		{ debug("Parser: math_exp"); 
                                $$ = concat(3, $1," ", $2);
                            }
	| math_exp LBRACE 		{ debug("Parser: math_exp"); 
                                $$ = concat(2, $1, "{");
                            }
	| math_exp RBRACE 		{ debug("Parser: math_exp"); 
                                $$ = concat(2, $1, "}");
                            }
	| STRING 				{ debug("Parser: math_exp"); 
                                $$ = $1; 
                            }
	| CHAR 					{ debug("Parser: math_exp"); 
                                $$ = $1; 
                            }
	| LBRACE 				{ debug("Parser: math_exp"); 
                                $$ = "{";
                            }
	| RBRACE 				{ debug("Parser: math_exp"); 
                                $$ = "}";
                            }
	;


normal_t:
    STRING                       { debug("Parser: normal_t"); 
                                   $$ = $1;
                                 } 
    | CHAR                       { debug("Parser: normal_t"); 
                                   $$ = $1; 
                                 } 
    | special_symbol             { debug("Parser: normal_t"); 
                                   $$ = $1; 
                                 } 
    | LBRACE                     { debug("Parser: normal_t"); 
                                   $$ = "{";
                                 }
    | RBRACE                     { debug("Parser: normal_t"); 
                                   $$ = "}";
                                 }
    ;

bold_text:
    STRING                     { debug("Parser: bold_text"); 
                                 $$ = $1;
                               } 
    | CHAR                     { debug("Parser: bold_text"); 
                                 $$ = $1;
                               } 
    | special_symbol           { debug("Parser: bold_text"); 
                                 $$ = $1;
                               } 
    | italic_t                 { debug("Parser: bold_text"); 
                                 $$ = concat(3, htmlGEN_italic_html_start, $1, htmlGEN_italic_html_end); 
                               } 
    | NEWLINES                 { debug("Parser: bold_text"); 
                                 $$ = ""; 
                               } 
    | bold_text STRING         { debug("Parser: bold_text"); 
                                 $$ = concat(3, $1," ", $2);
                               } 
    | bold_text CHAR           { debug("Parser: bold_text"); 
                                 $$ = concat(3, $1," ", $2);
                               } 
    | bold_text italic_t       { debug("Parser: bold_text"); 
                                 $$ = concat(5, $1," ", htmlGEN_italic_html_start, $2, htmlGEN_italic_html_end); 
                               } 
    | bold_text special_symbol { debug("Parser: bold_text"); 
                                 $$ = concat(3, $1," ", $2);
                               } 
    | bold_text NEWLINES       { debug("Parser: bold_text");
                                 $$ = $1;
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
    STRING                              { debug("Parser: italic_text"); 
                                          $$ = $1;
                                        }     
    | CHAR                              { debug("Parser: italic_text"); 
                                          $$ = $1;
                                        }     
    | special_symbol                    { debug("Parser: italic_text"); 
                                          $$ = $1;
                                        } 
    | bold_t                            { debug("Parser: italic_text"); 
                                          $$ = concat(3, htmlGEN_bold_html_start, $1, htmlGEN_bold_html_end); 
                                        }
    | NEWLINES                          { debug("Parser: italic_text"); 
                                          $$ = ""; 
                                        } 
    | italic_text STRING                { debug("Parser: italic_text"); 
                                          $$ = concat(3, $1," ", $2);
                                        }
    | italic_text CHAR                  { debug("Parser: italic_text"); 
                                          $$ = concat(3, $1," ", $2);
                                        }
    | italic_text bold_t                { debug("Parser: italic_text"); 
                                          $$ = concat(5, $1," ", htmlGEN_bold_html_start, $2, htmlGEN_bold_html_end); 
                                        }
    | italic_text special_symbol        { debug("Parser: italic_text"); 
                                          $$ = concat(3, $1," ", $2);
                                        } 
    | italic_text NEWLINES              { debug("Parser: italic_text");
                                          $$ = $1;
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
    error("Error: %s\n", errmsg);
    is_yyerror = 1;
}
 
int yywrap(void) { return 1; }
 
int main(int argc, char** argv)
{
    int is_error;

    info("Latex-HTML start.");
    htmlGEN_init(2); 
    
    // Define flag to start paragraph at first chance.
    is_p_start = 1;   
    avoid_p = 0;

    // Avoid cycles.
    is_bold = 0;
    is_italic = 0;

    // Control of itemize.
    list_level = 0;
    init_list_init(2);

    // Sintax error
    is_yyerror = 0;

    is_error = yyparse();

    if((is_error >= 0)&&(is_yyerror == 0)) {
        if(list_level == 0) {
            htmlGEN_print_all();
            info("HTML generated sucessfully.");
        }
        else {
            error("There are %d lists not closed.", list_level);
        }
    }
    htmlGEN_free();
    free_list_init();
    info("Latex-HTML end.");
    return 0;
}

int check_list_init(int level) {
    int original_size = is_list_init_size;
    int i;
    if( level > (is_list_init_size-1)) {
        while(level > (is_list_init_size-1)) {
            is_list_init_size = is_list_init_size*2;
        }
        is_list_init = realloc(is_list_init, is_list_init_size);  
        if(is_list_init == NULL) {
            error("Error during the realloc of push_list_init");
            return -1;
        }
        for(i=original_size; i<is_list_init_size; i++) {
            is_list_init[i] = 0;
        }
    }
    return 0;
}


int set_list_init(int value, int level) {
    if(check_list_init(level) < 0 ) {
        return -1;
    }
    is_list_init[level] = value;
    return 0;
}

int get_list_init(int level) {
    if(check_list_init(level) < 0 ) {
        return -1;
    }
    return is_list_init[level];
}

int init_list_init(int size) {
    is_list_init_size = size; 
    is_list_init = (int *) malloc(sizeof(int)*is_list_init_size); 
    if(is_list_init == NULL) {
        return - 1;
    }
    return 0;
}

void free_list_init() {
    free(is_list_init);
}
