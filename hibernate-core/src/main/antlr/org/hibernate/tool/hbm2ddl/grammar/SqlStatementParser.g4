parser grammar SqlStatementParser;

options {
	tokenVocab=SqlStatementLexer;
}

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl.grammar;
}


statements
    :   (statement)*
    ;

statement
	: (text)* STMT_END
	;

text :
 	WORD | QUOTED_TEXT
	;

