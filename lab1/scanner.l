%{
#include "parser.h"
#include "log.h"
%}

STRING  [A-Za-z0-9]+
WHITESPACE [ \n\r\t]+
ANY_CHAR .

%%

{WHITESPACE}                    /* ignore */ ;
"\\begin{document}"              {return BEGIN_DOC;}
"\\end{document}"                {return END_DOC; }
"\\begin{itemize}"               {return BEGIN_ITEM;}
"\\end{itemize}"                 {return END_ITEM;}
"\\begin{thebibliography}"       {return BEGIN_BIB;}
"\\end{thebibliography}"         {return END_BIB;}


"\\documentclass"    {return DOCCLASS;}
"\\usepackage"       {return USEPKG;}
"\\title"            {return TITLE;}
"\\author"           {return AUTHOR;}
"\\maketitle"        {return MAKETITLE;}
"\\textbf"           {return TXTBF;}
"\\textit"           {return TXTIT;}
"\\includegraphics"  {return INGRAPH;}
"\\cite"             {return CITE;}
"\\bibitem"          {return BBITEM;}
"\\item"             {return ITEM;}


"\\\\"              {return NEWLINE;}
"\$"                {return DOLLAR;}
"{"                 {return LBRACE;}
"}"                 {return RBRACE;}
"["                 {return LBRACKET;}
"]"                 {return RBRACKET;}


{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                debug("String received: %s", yylval.str);
                return STRING;
            }


{ANY_CHAR}  {
                yylval.str = (char *) strdup(yytext);
                debug("Char received: %s", yylval.str);
                return CHAR;
            }
