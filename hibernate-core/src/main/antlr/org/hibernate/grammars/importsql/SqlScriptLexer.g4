lexer grammar SqlScriptLexer;

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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