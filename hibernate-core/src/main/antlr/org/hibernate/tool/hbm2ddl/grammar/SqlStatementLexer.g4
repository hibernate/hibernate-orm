lexer grammar SqlStatementLexer;

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.hbm2ddl.grammar;

}

STMT_END
    : ';' ( '\t' | ' ' | '\r' | '\n' )*
    ;

MULTILINE_COMMENT
	:  '/*' .*? '*/' -> skip
	;

LINE_COMMENT
	: ('//' | '--') ~[\r\n]* -> skip
	;

NEWLINE
	: ('\r'? '\n' | '\r') -> skip
	;

WORD
	: ~[;]
	;

QUOTED_TEXT
	: '\'' ( ESCAPE_SEQUENCE | ~('\\'|'\'') )* '\''
	;

fragment
ESCAPE_SEQUENCE
	:	'\\' ('b'|'t'|'n'|'f'|'r'|'\\"'|'\''|'\\')
	|	UNICODE_ESCAPE
	|	OCTAL_ESCAPE
	;

fragment
OCTAL_ESCAPE
	:	'\\' ('0'..'3') ('0'..'7') ('0'..'7')
	|	'\\' ('0'..'7') ('0'..'7')
	|	'\\' ('0'..'7')
	;

fragment
UNICODE_ESCAPE
	:	'\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	;

fragment
HEX_DIGIT
	: ('0'..'9'|'a'..'f'|'A'..'F')
	;