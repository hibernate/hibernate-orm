lexer grammar OrderingLexer;


@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.ordering;
}

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Whitespace -> ignore
WS : ( ' ' | '\t' | '\f' | EOL ) -> skip;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// fragments and literals

fragment
EOL	: [\r\n]+;

fragment
DIGIT : [0-9];

INTEGER_LITERAL : INTEGER_NUMBER ;

fragment
INTEGER_NUMBER : ('0' | '1'..'9' DIGIT*) ;

LONG_LITERAL : INTEGER_NUMBER ('l'|'L');

BIG_INTEGER_LITERAL : INTEGER_NUMBER ('bi'|'BI') ;

HEX_LITERAL : '0' ('x'|'X') HEX_DIGIT+ ('l'|'L')? ;

fragment
HEX_DIGIT : (DIGIT|'a'..'f'|'A'..'F') ;

OCTAL_LITERAL : '0' ('0'..'7')+ ('l'|'L')? ;

FLOAT_LITERAL : FLOATING_POINT_NUMBER ('f'|'F')? ;

fragment
FLOATING_POINT_NUMBER
	: DIGIT+ '.' DIGIT* EXPONENT?
	| '.' DIGIT+ EXPONENT?
	| DIGIT+ EXPONENT
	| DIGIT+
	;

DOUBLE_LITERAL : FLOATING_POINT_NUMBER ('d'|'D') ;

BIG_DECIMAL_LITERAL : FLOATING_POINT_NUMBER ('bd'|'BD') ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? DIGIT+ ;

fragment SINGLE_QUOTE : '\'';
fragment DOUBLE_QUOTE : '"';

CHARACTER_LITERAL
	: SINGLE_QUOTE ( ESCAPE_SEQUENCE | SINGLE_QUOTE SINGLE_QUOTE | ~('\'') ) SINGLE_QUOTE
	;

STRING_LITERAL
	: DOUBLE_QUOTE ( ESCAPE_SEQUENCE | DOUBLE_QUOTE DOUBLE_QUOTE | ~('"') )* DOUBLE_QUOTE
	| SINGLE_QUOTE ( ESCAPE_SEQUENCE | SINGLE_QUOTE SINGLE_QUOTE | ~('\'') )* SINGLE_QUOTE
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


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// collation
COLLATE		: [cC] [oO] [lL] [lL] [aA] [tT] [eE];


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// NULL precedence

NULLS 	: [nN] [uU] [lL] [lL] [sS];
FIRST	: [fF] [iI] [rR] [sS] [tT];
LAST 	: [lL] [aA] [sS] [tT];


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// direction

ASC		: [aA] [sS] [cC] ( [eE] [nN] [dD] [iI] [nN] [gG] )?;
DESC	: [dD] [eE] [sS] [cC] ( [eE] [nN] [dD] [iI] [nN] [gG] )?;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Identifiers

fragment
LETTER : [a-zA-Z\u0080-\ufffe_$];

// Identifiers
IDENTIFIER
	: LETTER (LETTER | DIGIT)*
	;

fragment
BACKTICK : '`';

QUOTED_IDENTIFIER
	: BACKTICK ( ESCAPE_SEQUENCE | '\\' BACKTICK | ~([`]) )* BACKTICK
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// general tokens

OPEN_PAREN	: '(';
CLOSE_PAREN	: ')';

COMMA		: ',';
DOT			: '.';

PLUS 		: '+';
MINUS 		: '-';
MULTIPLY 	: '*';
DIVIDE 		: '/';
MODULO		: '%';

// Not used, but necessary for error reporting

EQUAL : '=';
NOT_EQUAL : '!=' | '^=' | '<>';
GREATER : '>';
GREATER_EQUAL : '>=';
LESS : '<';
LESS_EQUAL : '<=';

LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
AMPERSAND : '&';
SEMICOLON :	';';
COLON : ':';
PIPE : '|';
DOUBLE_PIPE : '||';
QUESTION_MARK :	'?';
ARROW :	'->';
BANG: '!';
AT: '@';
HASH: '#';