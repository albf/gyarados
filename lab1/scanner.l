%{
#include "parser.h"
%}

STRING  [a-z][a-z0-9_]*
WHITESPACE [ \n\r\t]+
NUM  [0-9]+
ANY_CHAR .

%%

{WHITESPACE} /* ignore */ ;
"\begin"            {return T_BEGIN;}
"\end"              {return T_END; }
"\documentclass"    {return T_DOCCLASS;}
"\title"            {return T_TITLE;}
"\author"           {return T_AUTHOR;}
"\maketitle"        {return T_MAKETITLE;}
"\textbf"           {return T_TXTBF;}
"\textit"           {return T_TXTIT;}
"\includegraphics"  {return T_INGRAPH;}

"\\\\"              {return NEWLINE;}
"{"                 {return LBRACE;}
"}"                 {return RBRACE;}
"["                 {return LBRACKET;}
"]"                 {return RBRACKET;}

"\cite{ref"         {return OPENCITE;}

{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                return T_STRING;
            }


{NUM}       {
                yylval.intval = atoi(yytext);
                return T_NUM;
            }

{ANY_CHAR}  {
                 printf ("Caracter %c (ASCII=%d) inesperado\n",
                         yytext[0], yytext[0]);
            }
