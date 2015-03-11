%{
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <stdlib.h>

/*#define MAX 1000

char table[MAX][MAX][MAX];

char *concat(int count, ...);
void selectPrime(char *);
void selectField(char *, char *);
void sucess(int *, int);*/

%}
 
%union{
    char *str;
    int  *intval;
}

%token <str> T_STRING
%token <intval> T_NUM
%token T_BEGIN
%token T_END
%token T_DOCCLASS
%token T_TITLE
%token T_AUTHOR
%token T_MAKETITLE
%token T_TXTBF
%token T_TXTIT
%token T_INGRAPH
%token T_OPENCITE;
%token NEWLINE
%token LBRACE
%token RBRACE
%token LBRACKET
%token RBRACKET

%type <str> create_stmt insert_stmt select_stmt col_list values_list 

%start stmt_list

%error-verbose
 
%%

stmt_list:  stmt_list stmt 
     |  stmt 
;

stmt:
        create_stmt ';' {printf("%s",$1);}
    |   insert_stmt ';' {printf("%s",$1);}
    |   select_stmt ';'
;

create_stmt:
       T_CREATE T_TABLE T_STRING '(' col_list ')'   {   FILE *F = fopen($3, "w"); 
                                fprintf(F, "%s\n", $5);
                                fclose(F);
                                $$ = concat(5, "\nCREATE TABLE: ", $3, "\nCOL_NAME: ", $5, "\n\n");
                            }
;

col_list:
        T_STRING        { $$ = $1; }
    |   col_list ',' T_STRING   { $$ = concat(3, $1, ";", $3); }
;


insert_stmt:
       T_INSERT T_INTO T_STRING T_VALUES '(' values_list ')' { FILE *F = fopen($3, "a"); 
                                  fprintf(F, "%s\n", $6);
                                  fclose(F);
                                  $$ = concat(5, "\nINSERT INTO TABLE: ", $3, "\nVALUES: ", $6, "\n\n");
                                }
;

values_list:
        T_STRING        { $$ = $1; }
    |   col_list ',' T_STRING   { $$ = concat(3, $1, ";", $3); }
;

select_stmt:
        T_SELECT '*' T_FROM T_STRING            { selectPrime($4); }


    |   T_SELECT col_list T_FROM T_STRING       { selectField($4, $2); }


 
%%
 
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

void selectPrime(char * tab) 
{
    FILE *F = fopen(tab, "r");
    printf("%s", concat(3, "\nSELECT '*' FROM: ", tab, "\n"));  
    if (F) {
        while ((getc(F) != EOF))
            printf("%c");
    }
    printf("\n");
}

void selectField(char *tab, char *fields) {
    FILE *F = fopen(tab, "r");
    int row=0, collum=0, size, fno=1, i=0, j, k=0, len;
    char c, *temp;
    char which[MAX][MAX], line[MAX];
    int index[MAX];

    // Get Fields name
    if(F) {
        while ((fscanf(F, "%s", line) != EOF)) {
            c = line[i];
            while (c != '\0') {
                switch(c) {
                    case ';':
                        table[row][collum][k] = '\0';
                        collum++;   k=0;
                        break;
                    default:
                        table[row][collum][k] = c;
                        k++;
                        break;
                }
                i++;
                c = line[i];
            }
            table[row][collum][k] = '\0';
            len = collum;
            collum=0;   k=0;    row++;
            i=0;
        }
        table[row][collum][k] = '\0';
        collum=0;
    }

    size = row; row=0; i=0;
    
    // Get the fields
    c = fields[i];
    while(c != '\0') {
        if(c != ';') {
            which[row][collum] = c;
            collum++;
        } else {
            which[row][collum] = '\0';
            row++;  collum=0;
        }
        i++;
        c = fields[i];
    }
    which[row][collum] = '\0';
    
    fno = row+1;  row = 0;

    printf("\nSELECT ");

    // Get which one in the table
    for (i=0; i<fno; i++) {
        if (i+1 != fno) {
            printf("%s, ", which[i]);
        } else {
            printf("%s ", which[i]);
        }

        temp = which[i];
        for (j=0; j < len; j++) {
            if (strcmp(temp, table[0][j]) == 0) {
                index[i]=j;
                break;
            }
        }
    }

    index[i] = -1;

    printf("FROM %s\n", tab);
    for (i=0; i<size; i++) {
        sucess(index, i);
    }
}

void sucess(int *index, int row) {
    int i=0;
    while(index[i] != -1) {
        printf("%s;", table[row][index[i]]);
        i++;
    }
    printf("\n");
}
