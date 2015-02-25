%{
	#include <stdio.h>
	#include <stdlib.h>
	#include "y.tab.h"
	extern YYSTYPE yylval;
%}
%%
[0-9]+		{ 
				if (yylval < 32768) {
					yylval = atoi(yytext);
					return NUM;
				} else
					REJECT;		/* Passa para a proxima regra */
			}

[a-z]		{
				yylval = *yytext - 'a';
				return ID;
			}

[+*()]		{ return *yytext; }

":="		{ return ASSIGN; }

";"			{ return SEMICOLON; }

print		{ return PRINT; }

[ \t\n]		;

.			{ yyerror("programa invalido!\n"); }

%%
