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

"\textit"       {return ITALIC;}

{WORD}          {
                    yylval.str = (char *) strdup(yytext);
                    return WORD;
                }
                    
