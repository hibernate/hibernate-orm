parser grammar SqlFormatterParser;

@header {
package org.hibernate.grammars.sql;
}

options {
    tokenVocab=SqlFormatterLexer;
}

// Minimal parser - we only use the lexer for formatting
// This parser is here to satisfy the build system
statement
    : .* EOF
    ;
