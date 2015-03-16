%{

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>
#define YYDEBUG 1

char *concat(int count, ...);

%}

%union {
    char *str;
}

%token <str>    WORD 
%token NEWLINE
%token ITALIC

%type <str> phrase paragraph parag_list text italic_t

%start body

%%

body:
    text                            {   printf("%s", $1);   }
    | italic_t                      {   printf("%s", $1);   }
    ;

italic_t:
    ITALIC '{' parag_list '}'       {   $$ = concat(3, "italico(", $3, ")");   }
    ;

text:
    parag_list                      {   $$ = $1;   }
    ;

phrase:
    WORD phrase                     {
                                        $$ = concat(3, $1, " ", $2);
                                    }
    |                               {   $$ = "";    }
    ;

paragraph:
    phrase NEWLINE                  {
                                        $$ = $1;
                                    }
    ;

parag_list:
    paragraph parag_list            {
                                        $$ = concat(3, $1, "\n", $2);
                                    }
    | phrase                        {
                                        $$ = $1;
                                    }
    ;

%%

int yyerror(const char* errmsg)
{
    printf("\n*** Erro: %s\n", errmsg);
}
 
int yywrap(void) { return 1; }
 
int main(int argc, char** argv)
{
     yydebug = 1;
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


static void yyprint (file, type, value)
        FILE *file;
        int type;
        YYSTYPE value;
{
        fprintf (file, " %s", value.str);
}
