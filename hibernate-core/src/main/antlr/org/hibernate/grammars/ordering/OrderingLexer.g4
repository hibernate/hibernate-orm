lexer grammar OrderingLexer;


@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

INTEGER_LITERAL : INTEGER_NUMBER ;

fragment
INTEGER_NUMBER : ('0' | '1'..'9' '0'..'9'*) ;

LONG_LITERAL : INTEGER_NUMBER ('l'|'L');

BIG_INTEGER_LITERAL : INTEGER_NUMBER ('bi'|'BI') ;

HEX_LITERAL : '0' ('x'|'X') HEX_DIGIT+ ('l'|'L')? ;

fragment
HEX_DIGIT : ('0'..'9'|'a'..'f'|'A'..'F') ;

OCTAL_LITERAL : '0' ('0'..'7')+ ('l'|'L')? ;

FLOAT_LITERAL : FLOATING_POINT_NUMBER ('f'|'F')? ;

fragment
FLOATING_POINT_NUMBER
	: ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
	| '.' ('0'..'9')+ EXPONENT?
	| ('0'..'9')+ EXPONENT
	| ('0'..'9')+
	;

DOUBLE_LITERAL : FLOATING_POINT_NUMBER ('d'|'D') ;

BIG_DECIMAL_LITERAL : FLOATING_POINT_NUMBER ('bd'|'BD') ;

fragment
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;

CHARACTER_LITERAL
	:	'\'' ( ESCAPE_SEQUENCE | ~('\''|'\\') ) '\'' {setText(getText().substring(1, getText().length()-1));}
	;

STRING_LITERAL
	:	'"' ( ESCAPE_SEQUENCE | ~('\\'|'"') )* '"' {setText(getText().substring(1, getText().length()-1));}
	|	('\'' ( ESCAPE_SEQUENCE | ~('\\'|'\'') )* '\'')+ {setText(getText().substring(1, getText().length()-1).replace("''", "'"));}
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

IDENTIFIER
	:	('a'..'z'|'A'..'Z'|'_'|'$'|'\u0080'..'\ufffe')('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|'\u0080'..'\ufffe')*
	;

QUOTED_IDENTIFIER
	: '`' ( ESCAPE_SEQUENCE | ~('\\'|'`') )* '`'
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// general tokens

OPEN_PAREN	: '(';
CLOSE_PAREN	: ')';

COMMA		: ',';
DOT			: '.';

PLUS 		: '+';
MINUS 		:	'-';
MULTIPLY 	: '*';
DIVIDE 		: '/';
MODULO		: '%';

