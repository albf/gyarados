#ifndef __htmlGEN_H__
#define __htmlGEN_H__

/* Functions: Interface for using this wonderfull lib. */

// Initialize data structures
// 0 - Okay ; -1 - Malloc error ; -2 - Second call for this function, not allowed.
int htmlGEN_init(int size);


// Add string to htmlGEN
// Return : 0 - Okay ; -1 ; Memory problem ; -2 : Null string passed, not allowed.
int htmlGEN_add_string(char * string, int is_bold, int is_italic, int is_par_start, int is_par_end);

// Initialize the bib, size is just initial size.
// Returns : 0 - Okay ; -1 - Malloc error ; -2 - Second function call, only one bib should be created. 
int htmlGEN_create_bib(int size);

// Add ref to htmlGEN
// Return : 0 - Okay ; -1 - Memory error ; -2 - Repeated entry. 
int htmlGEN_add_ref(char * new_index, char * new_ref);

// Print all strings, will check for refs automacally. 
// Return : 0 - okay ; -1 : Can't find ref ; -2 Bib not createad.
int htmlGEN_print_all();

// Free memory used by htmlGEN
void htmlGEN_free();

// Auxiliar functions, should be used only inside wonderland.

int htmlGEN_check_size();
char * htmlGEN_get_ref(char * index);
int htmlGEN_replace_ref(int index);

// HTML constants
#define htmlGEN_ref_symbol_start "\\cite{" 
#define htmlGEN_ref_symbol_end "}"
#define htmlGEN_header "<!DOCTYPE html>\n<html>\n<body>\n"
#define htmlGEN_footer "</body>\n</html>"

#define htmlGEN_par_html_start "<p>"
#define htmlGEN_par_html_end "</p>"

#define htmlGEN_bold_html_start "<b>"
#define htmlGEN_bold_html_end "</b>"

#define htmlGEN_italic_html_start "<i>"
#define htmlGEN_italic_html_end "</i>"

#endif //__htmlGEN_H__ 
