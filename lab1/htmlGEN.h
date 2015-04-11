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

// Set title from latex document.
// Return : 0 - okay ; -1 Can't allocate string ; -2 Error on strcopy
int htmlGEN_set_title (char * title);

// Return title value, if not setted, will return null;
char * htmlGEN_get_title();

// Include math header
void htmlGEN_include_math();

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

#define htmlGEN_image_html_start "<img src=\""
#define htmlGEN_image_html_middle "\" alt=\""
#define htmlGEN_image_html_end "\">"

#define htmlGEN_list_start "<ul>\n"
#define htmlGEN_list_end "</ul>"
#define htmlGEN_item_start "<li>"
#define htmlGEN_item_start_no_bullet "<li style=\"list-style-type:none\">"
#define htmlGEN_item_end "</li>"

#define htmlGEN_title_start "<h1>"
#define htmlGEN_title_end "</h1>"

#define htmlGEN_bib_title_start "<h2>"
#define htmlGEN_bib_title_end "</h1>"

#define htmlGEN_math_header_remote "<!DOCTYPE html>\n<html>\n<head>\n<!-- Copyright (c) 2009-2015 The MathJax Consortium -->\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n<!-- VERY IMPORTANT SCRIPTS-->\n<script type=\"text/x-mathjax-config\">\nMathJax.Hub.Config({\nextensions: [\"tex2jax.js\"],\njax: [\"input/TeX\",\"output/HTML-CSS\"],\ntex2jax: {inlineMath: [[\"$\",\"$\"],]}\n});\n</script>\n<script type=\"text/javascript\" src=\"https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML\">\nMathJax.Hub.Config({\ntex2jax: {\ninlineMath: [['$','$'], ['\\(','\\)']],\nprocessEscapes: true\n}\n});\n</script>\n</head>\n<body>"

#define htmlGEN_math_header "<!DOCTYPE html>\n<html>\n<head>\n<!-- Copyright (c) 2009-2015 The MathJax Consortium -->\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n<!-- VERY IMPORTANT SCRIPTS-->\n<script type=\"text/x-mathjax-config\">\nMathJax.Hub.Config({\nextensions: [\"tex2jax.js\"],\njax: [\"input/TeX\",\"output/HTML-CSS\"],\ntex2jax: {inlineMath: [[\"$\",\"$\"],]}\n});\n</script>\n<script type=\"text/javascript\" src=\"MathJax-2.5-latest/MathJax-2.5-latest/MathJax.js?config=TeX-AMS_HTML-full\">\nMathJax.Hub.Config({\ntex2jax: {\ninlineMath: [['$','$'], ['\\(','\\)']],\nprocessEscapes: true\n}\n});\n</script>\n</head>\n<body>"

#define htmlGEN_mathjax_path "MathJax-2.5-latest/MathJax-2.5-latest/MathJax.js"

#define htmlGEN_MAX_INT_SIZE 20
#define htmlGEN_use_ref_number 1

#endif //__htmlGEN_H__
