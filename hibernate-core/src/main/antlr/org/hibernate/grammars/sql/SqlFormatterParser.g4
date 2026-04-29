parser grammar SqlFormatterParser;

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
