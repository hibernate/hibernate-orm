lexer grammar HqlLexer;


@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hql.internal;
}

WS : WS_CHAR+ -> skip;

fragment
WS_CHAR : [ \f\t\r\n];

COMMENT : '/*' (~'*' | '*' ~'/' )* '*/' -> skip;

fragment
DIGIT : [0-9];

fragment
HEX_DIGIT : [0-9a-fA-F];

fragment
EXPONENT : [eE] [+-]? DIGIT+;

fragment
LONG_SUFFIX : [lL];

fragment
FLOAT_SUFFIX : [fF];

fragment
DOUBLE_SUFFIX : [dD];

fragment
BIG_DECIMAL_SUFFIX : [bB] [dD];

fragment
BIG_INTEGER_SUFFIX : [bB] [iI];

fragment
INTEGER_NUMBER
	: DIGIT+
	;

fragment
FLOATING_POINT_NUMBER
	: DIGIT+ '.' DIGIT* EXPONENT?
	| '.' DIGIT+ EXPONENT?
	| DIGIT+ EXPONENT
	| DIGIT+
	;

INTEGER_LITERAL : INTEGER_NUMBER;

LONG_LITERAL : INTEGER_NUMBER LONG_SUFFIX;

FLOAT_LITERAL : FLOATING_POINT_NUMBER FLOAT_SUFFIX?;

DOUBLE_LITERAL : FLOATING_POINT_NUMBER DOUBLE_SUFFIX;

BIG_INTEGER_LITERAL : INTEGER_NUMBER BIG_INTEGER_SUFFIX;

BIG_DECIMAL_LITERAL : FLOATING_POINT_NUMBER BIG_DECIMAL_SUFFIX;

HEX_LITERAL : '0' [xX] HEX_DIGIT+ LONG_SUFFIX?;

fragment SINGLE_QUOTE : '\'';
fragment DOUBLE_QUOTE : '"';

STRING_LITERAL
	: DOUBLE_QUOTE ( ~[\\"] | ESCAPE_SEQUENCE | DOUBLE_QUOTE DOUBLE_QUOTE )* DOUBLE_QUOTE
	  { setText(getText().substring(1, getText().length()-1).replace("\"\"", "\"")); }
	| SINGLE_QUOTE ( ~[\\'] | ESCAPE_SEQUENCE | SINGLE_QUOTE SINGLE_QUOTE )* SINGLE_QUOTE
	  { setText(getText().substring(1, getText().length()-1).replace("''", "'")); }
	;

fragment BACKSLASH : '\\';

fragment
ESCAPE_SEQUENCE
	: BACKSLASH [btnfr"']
	| BACKSLASH UNICODE_ESCAPE
	| BACKSLASH BACKSLASH
	;

fragment
UNICODE_ESCAPE
	: 'u' HEX_DIGIT HEX_DIGIT HEX_DIGIT HEX_DIGIT
	;

// ESCAPE start tokens
TIMESTAMP_ESCAPE_START : '{ts';
DATE_ESCAPE_START : '{d';
TIME_ESCAPE_START : '{t';

EQUAL : '=';
NOT_EQUAL : '!=' | '^=' | '<>';
GREATER : '>';
GREATER_EQUAL : '>=';
LESS : '<';
LESS_EQUAL : '<=';

COMMA :	',';
DOT	: '.';
LEFT_PAREN : '(';
RIGHT_PAREN	: ')';
LEFT_BRACKET : '[';
RIGHT_BRACKET : ']';
LEFT_BRACE : '{';
RIGHT_BRACE : '}';
PLUS : '+';
MINUS :	'-';
ASTERISK : '*';
SLASH : '/';
PERCENT	: '%';
AMPERSAND : '&';
SEMICOLON :	';';
COLON : ':';
PIPE : '|';
DOUBLE_PIPE : '||';
QUESTION_MARK :	'?';
ARROW :	'->';

// Keywords
ABS					: [aA] [bB] [sS];
AS					: [aA] [sS];
ALL					: [aA] [lL] [lL];
AND					: [aA] [nN] [dD];
ANY					: [aA] [nN] [yY];
ASC					: [aA] [sS] [cC];
AVG					: [aA] [vV] [gG];
BY					: [bB] [yY];
BETWEEN	 			: [bB] [eE] [tT] [wW] [eE] [eE] [nN];
BOTH				: [bB] [oO] [tT] [hH];
CASE				: [cC] [aA] [sS] [eE];
CAST				: [cC] [aA] [sS] [tT];
CEILING				: [cC] [eE] [iI] [lL] [iI] [nN] [gG];
CLASS				: [cC] [lL] [aA] [sS] [sS];
COALESCE			: [cC] [oO] [aA] [lL] [eE] [sS] [cC] [eE];
COLLATE				: [cC] [oO] [lL] [lL] [aA] [tT] [eE];
CONCAT				: [cC] [oO] [nN] [cC] [aA] [tT];
COUNT				: [cC] [oO] [uU] [nN] [tT];
CURRENT				: [cC] [uU] [rR] [rR] [eE] [nN] [tT];
CURRENT_DATE		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] [_] [dD] [aA] [tT] [eE];
CURRENT_INSTANT		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] [_] [iI] [nN] [sS] [tT] [aA] [nN] [tT];
CURRENT_TIME		: [cC] [uU] [rR] [rR] [eE] [nN] [tT] [_] [tT] [iI] [mM] [eE];
CURRENT_TIMESTAMP	: [cC] [uU] [rR] [rR] [eE] [nN] [tT] [_] [tT] [iI] [mM] [eE] [sS] [tT] [aA] [mM] [pP];
CROSS				: [cC] [rR] [oO] [sS] [sS];
DATE				: [dD] [aA] [tT] [eE];
DATETIME			: [dD] [aA] [tT] [eE] [tT] [iI] [mM] [eE];
DAY					: [dD] [aA] [yY];
DELETE				: [dD] [eE] [lL] [eE] [tT] [eE];
DESC				: [dD] [eE] [sS] [cC];
DISTINCT			: [dD] [iI] [sS] [tT] [iI] [nN] [cC] [tT];
ELEMENTS			: [eE] [lL] [eE] [mM] [eE] [nN] [tT] [sS];
ELSE				: [eE] [lL] [sS] [eE];
EMPTY				: [eE] [mM] [pP] [tT] [yY];
END					: [eE] [nN] [dD];
ENTRY				: [eE] [nN] [tT] [rR] [yY];
ESCAPE				: [eE] [sS] [cC] [aA] [pP] [eE];
EVERY				: [eE] [vV] [eE] [rR] [yY];
EXISTS				: [eE] [xX] [iI] [sS] [tT] [sS];
EXP	 				: [eE] [xX] [pP];
EXTRACT				: [eE] [xX] [tT] [rR] [aA] [cC] [tT];
FETCH				: [fF] [eE] [tT] [cC] [hH];
FLOOR				: [fF] [lL] [oO] [oO] [rR];
FROM				: [fF] [rR] [oO] [mM];
FOR					: [fF] [oO] [rR];
FORMAT				: [fF] [oO] [rR] [mM] [aA] [tT];
FULL				: [fF] [uU] [lL] [lL];
FUNCTION			: [fF] [uU] [nN] [cC] [tT] [iI] [oO] [nN];
GREATEST			: [gG] [rR] [eE] [aA] [tT] [eE] [sS] [tT];
GROUP				: [gG] [rR] [oO] [uU] [pP];
HAVING				: [hH] [aA] [vV] [iI] [nN] [gG];
HOUR				: [hH] [oO] [uU] [rR];
IFNULL				: [iI] [fF] [nN] [uU] [lL] [lL];
IN					: [iI] [nN];
INDEX				: [iI] [nN] [dD] [eE] [xX];
INNER				: [iI] [nN] [nN] [eE] [rR];
INSERT				: [iI] [nN] [sS] [eE] [rR] [tT];
INSTANT				: [iI] [nN] [sS] [tT] [aA] [nN] [tT];
INTO 				: [iI] [nN] [tT] [oO];
IS					: [iI] [sS];
JOIN				: [jJ] [oO] [iI] [nN];
KEY					: [kK] [eE] [yY];
LEADING				: [lL] [eE] [aA] [dD] [iI] [nN] [gG];
LEAST				: [lL] [eE] [aA] [sS] [tT];
LEFT				: [lL] [eE] [fF] [tT];
LENGTH				: [lL] [eE] [nN] [gG] [tT] [hH];
LIMIT				: [lL] [iI] [mM] [iI] [tT];
LIKE				: [lL] [iI] [kK] [eE];
LIST				: [lL] [iI] [sS] [tT];
LN  				: [lL] [nN];
LOCATE				: [lL] [oO] [cC] [aA] [tT] [eE];
LOWER				: [lL] [oO] [wW] [eE] [rR];
MAP					: [mM] [aA] [pP];
MAX					: [mM] [aA] [xX];
MAXELEMENT			: [mM] [aA] [xX] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MAXINDEX			: [mM] [aA] [xX] [iI] [nN] [dD] [eE] [xX];
MEMBER				: [mM] [eE] [mM] [bB] [eE] [rR];
MIN					: [mM] [iI] [nN];
MINELEMENT			: [mM] [iI] [nN] [eE] [lL] [eE] [mM] [eE] [nN] [tT];
MININDEX			: [mM] [iI] [nN] [iI] [nN] [dD] [eE] [xX];
MINUTE				: [mM] [iI] [nN] [uU] [tT] [eE];
MOD					: [mM] [oO] [dD];
MONTH				: [mM] [oO] [nN] [tT] [hH];
NANOSECOND			: [nN] [aA] [nN] [oO] [sS] [eE] [cC] [oO] [nN] [dD];
NEW					: [nN] [eE] [wW];
NOT					: [nN] [oO] [tT];
NULLIF				: [nN] [uU] [lL] [lL] [iI] [fF];
OBJECT				: [oO] [bB] [jJ] [eE] [cC] [tT];
OF					: [oO] [fF];
OFFSET				: [oO] [fF] [fF] [sS] [eE] [tT];
ON					: [oO] [nN];
OR					: [oO] [rR];
ORDER				: [oO] [rR] [dD] [eE] [rR];
OUTER				: [oO] [uU] [tT] [eE] [rR];
OVERLAY				: [oO] [vV] [eE] [rR] [lL] [aA] [yY];
PLACING				: [pP] [lL] [aA] [cC] [iI] [nN] [gG];
POSITION			: [pP] [oO] [sS] [iI] [tT] [iI] [oO] [nN];
POWER				: [pP] [oO] [wW] [eE] [rR];
QUARTER				: [qQ] [uU] [aA] [rR] [tT] [eE] [rR];
REPLACE				: [rR] [eE] [pP] [lL] [aA] [cC] [eE];
RIGHT				: [rR] [iI] [gG] [hH] [tT];
ROUND				: [rR] [oO] [uU] [nN] [dD];
SECOND				: [sS] [eE] [cC] [oO] [nN] [dD];
SELECT				: [sS] [eE] [lL] [eE] [cC] [tT];
SET					: [sS] [eE] [tT];
SIGN				: [sS] [iI] [gG] [nN];
SIZE				: [sS] [iI] [zZ] [eE];
SOME				: [sS] [oO] [mM] [eE];
SQRT				: [sS] [qQ] [rR] [tT];
STR					: [sS] [tT] [rR];
SUBSTRING			: [sS] [uU] [bB] [sS] [tT] [rR] [iI] [nN] [gG];
SUM					: [sS] [uU] [mM];
THEN				: [tT] [hH] [eE] [nN];
TIME				: [tT] [iI] [mM] [eE];
TIMESTAMP			: [tT] [iI] [mM] [eE] [sS] [tT] [aA] [mM] [pP];
TRAILING			: [tT] [rR] [aA] [iI] [lL] [iI] [nN] [gG];
TREAT				: [tT] [rR] [eE] [aA] [tT];
TRIM				: [tT] [rR] [iI] [mM];
TYPE				: [tT] [yY] [pP] [eE];
UPDATE				: [uU] [pP] [dD] [aA] [tT] [eE];
UPPER				: [uU] [pP] [pP] [eE] [rR];
VALUE				: [vV] [aA] [lL] [uU] [eE];
WEEK				: [wW] [eE] [eE] [kK];
WHEN				: [wW] [hH] [eE] [nN];
WHERE				: [wW] [hH] [eE] [rR] [eE];
WITH				: [wW] [iI] [tT] [hH];
YEAR				: [yY] [eE] [aA] [rR];

ASIN				: [aA] [sS] [iI] [nN];
ATAN				: [aA] [cC] [oO] [sH];
ATAN2				: [aA] [tT] [aA] [nN] [2];
ACOS				: [aA] [tT] [aA] [nN];
SIN					: [sS] [iI] [nN];
COS					: [cC] [oO] [sH];
TAN					: [tT] [aA] [nN];

// case-insensitive true, false and null recognition (split vote :)
TRUE 	: [tT] [rR] [uU] [eE];
FALSE 	: [fF] [aA] [lL] [sS] [eE];
NULL 	: [nN] [uU] [lL] [lL];


fragment
LETTER : [a-zA-Z\u0080-\ufffe_$];

// Identifiers
IDENTIFIER
	: LETTER (LETTER | DIGIT)*
	;

fragment
BACKTICK : '`';

QUOTED_IDENTIFIER
	: BACKTICK ( ~([\\`]) | ESCAPE_SEQUENCE )* BACKTICK
	;
