%{
#include "parser.h"
%}

STRING  [a-z][a-z0-9]*
WHITESPACE [ \n\r\t]+
NUM  [0-9]+
ANY_CHAR .

%%

{WHITESPACE}        /* ignore */ ;
"\begin"            {return T_BEGIN;}
"\end"              {return T_END; }
"\documentclass"    {return DOCCLASS;}
"\usepackage"       {return USEPKG;}
"\title"            {return TITLE;}
"\author"           {return AUTHOR;}
"\maketitle"        {return MAKETITLE;}
"\textbf"           {return TXTBF;}
"\textit"           {return TXTIT;}
"\includegraphics"  {return INGRAPH;}
"\cite"             {return CITE;}
"\bibitem"          {return BBITEM;}
"\item"             {return ITEM;}

"\\\\"              {return NEWLINE;}
"\$"                {return DOLLAR;}
"{"                 {return LBRACE;}
"}"                 {return RBRACE;}
"["                 {return LBRACKET;}
"]"                 {return RBRACKET;}

{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                return STRING;
            }

{NUM}       {
                yylval.intval = atoi(yytext);
                return NUM;
            }

{ANY_CHAR}  {
                yylval.str = (char *) strdup(yytext);
                return CHAR;
            }
