%{
#include "parser.h"
%}

WHITESPACE  [ \n\r\t]+
WORD        [^ \n\r\t]+

%%

{WHITESPACE}    ;

"\\\\"          {return NEWLINE; }

{WORD}          {
                    yylval.str = (char *) strdup(yytext);
                    return WORD;
                }
                    
