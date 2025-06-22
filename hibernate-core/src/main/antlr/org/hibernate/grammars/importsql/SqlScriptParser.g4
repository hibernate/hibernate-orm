parser grammar SqlScriptParser;

options {
	tokenVocab=SqlScriptLexer;
}

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.importsql;
}

script
	: (NEWLINE | SPACE | TAB)* ( commandBlock (NEWLINE | SPACE | TAB)* )* EOF
	;

commandBlock
	: command DELIMITER
	;

command
	: ( CHAR | QUOTED_TEXT ) // The first part must be a non-whitespace
	  (
	    ( CHAR | QUOTED_TEXT | SPACE | TAB ) // Following chars may include spaces
	    NEWLINE* // And also newlines in betweeen
	  )*
	;

