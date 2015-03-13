
#include <stdio.h>
#include <stdlib.h> 
#include <string.h>


/* Global Variables: One should not touch these outside this file. */

int htmlGEN_is_init = 0;                // Indicate if init was already called.
char ** htmlGEN_result;                 // Vector of HTML strings, will print all in order as final result.
int * htmlGEN_is_there_ref;             // Indicates if nth line has a ref (1) or not (0).
int * htmlGEN_is_bold;                  // Indicates if nth line has is bold (1) or not (0).
int * htmlGEN_is_italic;                // Indicates if nth line has is italic (1) or not (0).
int * htmlGEN_is_par_start;             // Indicates if nth line start a paragraph (1) or not (0).
int * htmlGEN_is_par_end;               // Indicates if nth line end a paragraph (1) or not (0).
int htmlGEN_counter;                    // Current line counter.
int htmlGEN_size;                       // Current size of vectors.
int htmlGEN_is_there_bib;               // Indicate if there is bib.
char ** htmlGEN_ref_bank;               // Bank of refs.
char ** htmlGEN_ref_index;              // Ref index from ref_bank.
int htmlGEN_ref_counter;                // Current number of refs.
int htmlGEN_ref_size;                   // Current size of ref vectors.

const char htmlGEN_ref_symbol_start [] = "\\cite{"; 
const char htmlGEN_ref_symbol_end [] = "}";
const char htmlGEN_header [] = "<!DOCTYPE html>\n<html>\n<body>\n";
const char htmlGEN_footer [] = "</body>\n</html>";

const char htmlGEN_par_html_start [] = "<p>";
const char htmlGEN_par_html_end [] = "</p>";

const char htmlGEN_bold_html_start [] = "<b>";
const char htmlGEN_bold_html_end [] = "</b>";

const char htmlGEN_italic_html_start [] = "<i>";
const char htmlGEN_italic_html_end [] = "</i>";

/* Functions: Interface for using this wonderfull lib. */

int htmlGEN_init(int size);
int htmlGEN_check_size();
void htmlGEN_free();
void htmlGEN_add_string(char * string, int is_bold, int is_italic, int is_par_start, int is_par_end);
void htmlGEN_print_all();
int htmlGEN_create_bib(int size);
char * htmlGEN_get_ref(char * index);
int htmlGEN_add_ref(char * new_index, char * new_ref);

// Example function
int main () {
    // Initialize
    htmlGEN_init(2);

    // Add string as they come. 
    htmlGEN_add_string("Hi there\n", 0, 0, 0, 0);
    htmlGEN_add_string("My name is Brian\n", 0, 0, 0, 0);
    htmlGEN_add_string("I will graduate next semesters,", 0, 0, 0, 0);
    htmlGEN_add_string(" at least I think so.", 0, 0, 0, 0);
    htmlGEN_add_string("\n", 0, 0, 0, 0);

    // Create bib
    htmlGEN_create_bib(2);

    // Add ref
    htmlGEN_add_ref("1", "ABC\n");

    // Add first ref as a string, for test.
    htmlGEN_add_string(htmlGEN_get_ref("1"), 0, 0, 0, 0);

    // Print all the strings inserted.
    htmlGEN_print_all();

    // Free memory.
    htmlGEN_free();

    return 0;
}

// Initialize data structures
// 0 - Okay ; -1 - Malloc error ; -2 - Second call for this function, not allowed.
int htmlGEN_init(int size) {
    if(htmlGEN_is_init > 0) {
        printf("Only one htmlGEN_init is allowed.\n");   
        return -2;
    }

    htmlGEN_result = (char **) malloc (sizeof(char *)*size); 
    htmlGEN_is_there_ref = (int *) malloc (sizeof(int)*size);
    htmlGEN_is_bold = (int *) malloc (sizeof(int)*size);
    htmlGEN_is_italic = (int *) malloc (sizeof(int)*size);
    htmlGEN_is_par_start = (int *) malloc (sizeof(int)*size);
    htmlGEN_is_par_end = (int *) malloc (sizeof(int)*size);
    htmlGEN_counter = 0;
    htmlGEN_size = size;
    htmlGEN_is_there_bib = 0;
    htmlGEN_is_init = 1;

    if((htmlGEN_result == NULL) || (htmlGEN_is_there_ref == NULL)) {
            printf("Problem allocating memory for htmlGEN. #0\n");
            return -1;
    }

    return 0;
}

// Check current size from structures. Doubles it if full.
int htmlGEN_check_size(){
    if(htmlGEN_counter == (htmlGEN_size - 1)) {
        htmlGEN_size = htmlGEN_size*2;
        htmlGEN_result = realloc (htmlGEN_result, sizeof(char *)*htmlGEN_size); 
        htmlGEN_is_there_ref = realloc (htmlGEN_is_there_ref, sizeof(int)*htmlGEN_size);
        htmlGEN_is_bold = realloc (htmlGEN_is_bold, sizeof(int)*htmlGEN_size);
        htmlGEN_is_italic = realloc (htmlGEN_is_italic, sizeof(int)*htmlGEN_size);

        if((htmlGEN_result == NULL) || (htmlGEN_is_there_ref == NULL)) {
                printf("Problem allocating memory for htmlGEN #1.\n");
                return -1;
        }
    }
    if(htmlGEN_is_there_bib > 0) {
        if(htmlGEN_ref_counter == (htmlGEN_ref_size - 1)) {
            htmlGEN_ref_size = htmlGEN_ref_size*2;
            htmlGEN_ref_bank = realloc (htmlGEN_ref_bank, sizeof(char *)*htmlGEN_ref_size); 
            htmlGEN_ref_index = realloc (htmlGEN_ref_index, sizeof(int)*htmlGEN_ref_size);

            if((htmlGEN_ref_bank == NULL) || (htmlGEN_ref_index == NULL)) {
                    printf("Problem allocating memory for htmlGEN #2.\n");
                    return -1;
            }
        }
    }
    return 0;
}

// Free memory used by htmlGEN
void htmlGEN_free() {
    int i;

    if(htmlGEN_is_init == 0) {
        printf("htmlGEN was not initialized.\n");
        return; 
    }

    for (i=0; i<htmlGEN_counter; i++) {
        free(htmlGEN_result[i]);
    }

    free(htmlGEN_result);
    free(htmlGEN_is_there_ref); 

    if(htmlGEN_is_there_bib > 0) {
        for (i=0; i<htmlGEN_ref_counter; i++) {
            free(htmlGEN_ref_bank[i]);
            free(htmlGEN_ref_index[i]);
        }
        free(htmlGEN_ref_bank);
        free(htmlGEN_ref_index);
    }
    return;
}

// Add string to htmlGEN
void htmlGEN_add_string(char * string, int is_bold, int is_italic, int is_par_start, int is_par_end) {
    int is_there_ref = 0;
    int i;
    
    // Check if there is space available
    if(htmlGEN_check_size() < 0) {
            return; 
    }
    if(string == NULL) {
        printf("Null string passed to htmlGEN.");
        return;
    }
    
    if(strstr(string, htmlGEN_ref_symbol_start) != NULL) {
        is_there_ref = 1;
    }

    // Copy string and ref flag. Update counter after that. 
    htmlGEN_result[htmlGEN_counter] = (char *) malloc (sizeof(char)*strlen(string)); 
    strcpy (htmlGEN_result[htmlGEN_counter], string);
    htmlGEN_is_there_ref[htmlGEN_counter] = is_there_ref;
    htmlGEN_is_bold[htmlGEN_counter] = is_bold;
    htmlGEN_is_italic[htmlGEN_counter] = is_italic;
    htmlGEN_counter++;
}

// Print all strings.
void htmlGEN_print_all() {
    int i;
    
    // Print header
    printf("%s", htmlGEN_header);

    for(i=0; i<htmlGEN_counter; i++) {
            // Print italic and bold HTML code if they are required, and the text itself.
            if(htmlGEN_is_bold[i]>0) {
                printf("%s", htmlGEN_bold_html_start);
            }
            if (htmlGEN_is_italic[i]>0) {
                printf("%s", htmlGEN_italic_html_start);
            }
            
            printf("%s", htmlGEN_result[i]);
            
            if(htmlGEN_is_bold[i]>0) {
                printf("%s", htmlGEN_bold_html_end);
            }
            if (htmlGEN_is_italic[i]>0) {
                printf("%s", htmlGEN_italic_html_end);
            }
    }
    
    // Print end
    printf("%s", htmlGEN_ref_symbol_end);
    printf("\n");
    
}

// Initialize the bib, size is just initial size.
// Returns : 0 - Okay ; -1 - Malloc error ; -2 - Second function call, only one bib should be created. 
int htmlGEN_create_bib(int size) {
    if(htmlGEN_is_there_bib > 1) {
        printf("Only one bib is allowed.\n");
        return -2;
    }

    htmlGEN_ref_bank = (char **) malloc (sizeof(char *)*size); 
    htmlGEN_ref_index = (char **) malloc (sizeof(char *)*size);
    htmlGEN_ref_counter = 0;
    htmlGEN_ref_size = size;
    htmlGEN_is_there_bib = 1;

    if((htmlGEN_ref_bank == NULL) || (htmlGEN_ref_index == NULL)) {
            printf("Problem allocating memory for htmlGEN. #3\n");
            return -1;
    }

    return 0;
}

// Get ref using index, NULL if don't exist. Assume that there each index is unique.
char * htmlGEN_get_ref(char * index) {
    char * ref = NULL; 
    int i;

    if(htmlGEN_is_there_bib > 0) {
        for(i=0; i<htmlGEN_ref_counter; i++) {
            if(strcmp(htmlGEN_ref_index[i],index)==0) {
                ref = htmlGEN_ref_bank[i];
                break;
            }
        }
    }

    return ref;
}

// Add ref to htmlGEN
// Return : 0 - Okay ; -1 - Memory error ; -2 - Repeated entry. 
int htmlGEN_add_ref(char * new_index, char * new_ref) {
    // Check if there is space available
    if(htmlGEN_check_size() < 0) {
            return -1; 
    }

    if(htmlGEN_get_ref(new_index)!=NULL) {
        printf("Repeated reference (%s) in bib.\n", new_index);
        return -2;
    }

    // Copy string and ref flag. Update counter after that. 
    htmlGEN_ref_bank[htmlGEN_ref_counter] = (char *) malloc (sizeof(char)*strlen(new_ref)); 
    strcpy (htmlGEN_ref_bank[htmlGEN_ref_counter], new_ref);
    htmlGEN_ref_index[htmlGEN_ref_counter] = (char *) malloc (sizeof(char)*strlen(new_index)); 
    strcpy (htmlGEN_ref_index[htmlGEN_ref_counter], new_index);
    htmlGEN_ref_counter++;
    return 0;
}
