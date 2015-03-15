%{
#include "parser.h"
%}

WHITESPACE  [ \n\r\t]+
WORD        [^ \n\r\t]+

%%

{WHITESPACE}    ;

"\\\\"          {return NEWLINE; }
"{"             {return '{';}
"}"             {return '}';}

"\textbf"       {return BOLD;}
"\textit"       {return ITALIC;}

{WORD}          {
                    yylval.str = (char *) strdup(yytext);
                    return WORD;
                }
                    
