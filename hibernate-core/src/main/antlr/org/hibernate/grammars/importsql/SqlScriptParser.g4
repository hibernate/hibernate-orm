parser grammar SqlScriptParser;

options {
	tokenVocab=SqlScriptLexer;
}

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

