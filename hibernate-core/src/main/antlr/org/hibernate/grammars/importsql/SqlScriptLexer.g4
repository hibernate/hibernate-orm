lexer grammar SqlScriptLexer;

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.importsql;
}

LINE_COMMENT
	: ('//' | '--') ~[\r\n]* -> skip
	;

MULTILINE_COMMENT
	:  '/*'
	(
		{ getInputStream().LA(2)!='/' }? '*'
		| '\r' '\n'
		| '\r'
		| '\n'
		| ~('*'|'\n'|'\r')
	)*
	'*/' -> skip
	;

CHAR
	: ~( ';' | '\n' | '\r' | ' ' | '\t')
	;

SPACE
	: ' '
	;

TAB
	: '\t'
	;

NEWLINE
	: ('\r'? '\n' | '\r')
	;

DELIMITER:
	';'
	;

QUOTED_TEXT
	: '`' ( ~('`') )*? '`'
	| '\'' ( ('\'''\'') | ~('\'') )*? '\''
	;