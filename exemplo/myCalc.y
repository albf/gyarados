%{
	#include <stdio.h>
	int yylex(void);
	void yyerror(char *);
	int sym[26];
%}

%token NUM ID ASSIGN SEMICOLON PRINT

%left '+' 
%left '*'

%%

statement:
		command SEMICOLON statement
		|
		;

command:
		ID ASSIGN expression				{ sym[$1] = $3; }
		| PRINT '(' ID ')'					{ printf("%d\n", $3); }
		;

expression:
		term '+' term						{ $$ = $1 + $3; }	
		;

term:
		factor '*' factor					{ $$ = $1 * $3; }
		;

factor:
		ID									{ $$ = sym[$1]; }
		| NUM								{ $$ = $1; }
		| '(' expression ')'				{ $$ = $2; }
		;

%%

void yyerror(char *s) {
	fprintf(stderr, "%s\n", s);
}

int yywrap() { return 1; }

int main(void) {
	yyparse();
	return 0;
}
