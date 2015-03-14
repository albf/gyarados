%{

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

char *concat(int count, ...);

%}

%union {
    char *str;
    char alfa;
}

%token <str>    STRING
%token <alfa>   CHAR
%token NEWLINE

%type <str> phrase paragraph

%start text

%%

text:
    phrase                          {printf("%s\n", $1);}
    | paragraph text                {printf("%s\n", $1);}
    ;

phrase:
    CHAR phrase                     {$$ = concat(2, $1, $2);}
    | STRING phrase                 {$$ = concat(2, $1, $2);}
    |                               {$$ = "";}
    ;

paragraph:
    phrase NEWLINE                  {$$ = concat(2, $1, '\n');}

%%

int yyerror(const char* errmsg)
{
    printf("\n*** Erro: %s\n", errmsg);
}
 
int yywrap(void) { return 1; }
 
int main(int argc, char** argv)
{
     yyparse();
     return 0;
}

char* concat(int count, ...)
{
    va_list ap;
    int len = 1, i;

    va_start(ap, count);
    for(i=0 ; i<count ; i++)
        len += strlen(va_arg(ap, char*));
    va_end(ap);

    char *result = (char*) calloc(sizeof(char),len);
    int pos = 0;

    // Actually concatenate strings
    va_start(ap, count);
    for(i=0 ; i<count ; i++)
    {
        char *s = va_arg(ap, char*);
        strcpy(result+pos, s);
        pos += strlen(s);
    }
    va_end(ap);

    return result;
}
