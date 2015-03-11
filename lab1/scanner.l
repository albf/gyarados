%{
#include "parser.h"
%}

STRING  [a-z][a-z0-9_]*
WHITESPACE [ \n\r\t]+
NUM  [0-9]+
ANY_CHAR .

%%

{WHITESPACE} /* ignore */ ;
"\begin"    {return T_SELECT;}
"\end"      {return T_FROM; }
"\documentclass"    {return T_CREATE;}
"\title"     {return T_TABLE;}
"\author"    {return T_INSERT;}
"\maketitle"      {return T_INTO;}
"\textbf"    {return T_VALUES;}
"\textit"         {return '(';}
"\includegraphics"         {return ')';}
"document"
"itemize"

"\\\\"         {return '*';}
"{"
"}"
"["
"]"

"\cite{ref"         {return ',';}

{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                return T_STRING;
            }


{NUM}

{ANY_CHAR}  {
                 printf ("Caracter %c (ASCII=%d) inesperado\n", yytext[0], yytext[0]);
            }

