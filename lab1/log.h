#ifndef __SPITZ_LOG_H__
#define __SPITZ_LOG_H__

#include <stdio.h>

void info(const char *msg, ...);

void debug(const char *msg, ...);

void warning(const char *msg, ...);

void error(const char *msg, ...);

#endif /* __SPITZ_LOG_H__ */
