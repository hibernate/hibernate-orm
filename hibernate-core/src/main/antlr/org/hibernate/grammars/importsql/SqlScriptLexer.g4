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
	:  '/*' .*? '*/' -> skip
	;

NEWLINE
	: ('\r'? '\n' | '\r') -> skip
	;

STMT_END
    : ';' ( '\t' | ' ' | '\r' | '\n' )*
    ;

NOT_STMT_END
    : ~[;]
    ;

QUOTED_TEXT
	: '`' .*? '`'
	;

//WORD
//	: ~[;]
//	;
//
//QUOTED_TEXT
//	: '\'' ( ESCAPE_SEQUENCE | ~('\\'|'\'') )* '\''
//	;
//
//fragment
//ESCAPE_SEQUENCE
//	:	'\\' ('b'|'t'|'n'|'f'|'r'|'\\"'|'\''|'\\')
//	|	UNICODE_ESCAPE
//	|	OCTAL_ESCAPE
//	;
//
//fragment
//OCTAL_ESCAPE
//	:	'\\' ('0'..'3') ('0'..'7') ('0'..'7')
//	|	'\\' ('0'..'7') ('0'..'7')
//	|	'\\' ('0'..'7')
//	;
//
//fragment
//UNICODE_ESCAPE
//	:	'\\' 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
//	;
//
//fragment
//HEX_DIGIT
//	: ('0'..'9'|'a'..'f'|'A'..'F')
//	;