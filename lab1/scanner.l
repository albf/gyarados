%{
#include "parser.h"
#include "log.h"
%}

STRING  [A-Za-z0-9.,?!\\^()]+
WHITESPACE [ \r\t]+
NEWLINES [\n]+
SPECIAL_CHAR [$]
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


"\\\\"              {return NEWLINES;}
"\$"                {return DOLLAR;}
"{"                 {return LBRACE;}
"}"                 {return RBRACE;}
"["                 {return LBRACKET;}
"]"                 {return RBRACKET;}

{NEWLINES}          { return NEWLINES; }


{STRING}    {  
                yylval.str = (char *) strdup(yytext);
                debug("Lexer: String received: %s", yylval.str);
                return STRING;
            }

"\\"{SPECIAL_CHAR}	{
						yylval.str = (char *) strdup(yytext+1);
						debug("Lexer: Special Char received: %s", yylval.str);
						return CHAR;
					}

{ANY_CHAR}  {
                yylval.str = (char *) strdup(yytext);
                debug("Lexer: Char received: %s", yylval.str);
                return CHAR;
            }
