%{
#include "parser.h"
%}

STRING  [a-z0-9]+
WHITESPACE [ \n\r\t]+
ANY_CHAR .

%%

"\\\\"      { return NEWLINE; }

{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                return STRING;
            }


{ANY_CHAR}  {
                yylval.alfa = *(yytext);
                return CHAR;
            }
