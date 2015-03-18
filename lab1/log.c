/*
    Copyright 2014 Ian Liu Rodrigues <ian.liu@ggaunicamp.com> (autor original do sistema de cores)
    Copyright 2015 Alexandre Luiz Brisighello Filho <albf.unicamp@gmail.com> 
*/

#include "log.h"

#include <unistd.h>
#include <stdarg.h>

#define LOG_LEVEL 1

enum color {
	RED,
	GREEN,
	YELLOW,
	BLUE,
	NORMAL
};

const char *color_codes[] = {
	"\033[1;31m",
	"\033[1;32m",
	"\033[1;33m",
	"\033[1;34m",
	"\033[0m",
};

static void vmessage(FILE *f, const char *msg, const char *prefix,
		enum color color, va_list ap)
{
	if (prefix) {
		if (isatty(fileno(f)))
			fprintf(f, "%s%s%s ",
					color_codes[color],
					prefix,
					color_codes[NORMAL]);
		else
			fprintf(f, "%s ", prefix);
	}
	vfprintf(f, msg, ap);
	fprintf(f, "\n");
}

void info(const char *msg, ...)
{
	if (LOG_LEVEL < 1)
		return;
	
	va_list ap;
	va_start(ap, msg);
	vmessage(stderr, msg, "info:", BLUE, ap);
	va_end(ap);
}

void debug(const char *msg, ...)
{
	if (LOG_LEVEL < 2)
		return;
	
	va_list ap;
	va_start(ap, msg);
	vmessage(stderr, msg, "debug:", GREEN, ap);
	va_end(ap);
}

void warning(const char *msg, ...)
{
	va_list ap;
	va_start(ap, msg);
	vmessage(stderr, msg, "warning:", YELLOW, ap);
	va_end(ap);
}

void error(const char *msg, ...)
{
	va_list ap;
	va_start(ap, msg);
	vmessage(stderr, msg, "error:", RED, ap);
	va_end(ap);
}
