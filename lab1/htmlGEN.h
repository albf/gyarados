#ifndef __htmlGEN_H__
#define __htmlGEN_H__

/* Functions: Interface for using this wonderfull lib. */

int htmlGEN_init(int size);
int htmlGEN_check_size();
void htmlGEN_free();
void htmlGEN_add_string(char * string, int is_bold, int is_italic, int is_par_start, int is_par_end);
void htmlGEN_print_all();
int htmlGEN_create_bib(int size);
char * htmlGEN_get_ref(char * index);
int htmlGEN_add_ref(char * new_index, char * new_ref);


#endif //__htmlGEN_H__ 
