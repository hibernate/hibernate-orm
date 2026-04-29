lexer grammar SqlFormatterLexer;

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.sql;
}

// Comments (preserve them in output)
LINE_COMMENT
    : '--' ~[\r\n]*
    ;

BLOCK_COMMENT
    : '/*' .*? '*/'
    ;

// Keywords - ordered by frequency and importance
SELECT      : [sS][eE][lL][eE][cC][tT];
FROM        : [fF][rR][oO][mM];
WHERE       : [wW][hH][eE][rR][eE];
JOIN        : [jJ][oO][iI][nN];
LEFT        : [lL][eE][fF][tT];
RIGHT       : [rR][iI][gG][hH][tT];
INNER       : [iI][nN][nN][eE][rR];
OUTER       : [oO][uU][tT][eE][rR];
FULL        : [fF][uU][lL][lL];
CROSS       : [cC][rR][oO][sS][sS];
LATERAL     : [lL][aA][tT][eE][rR][aA][lL];
ON          : [oO][nN];
AND         : [aA][nN][dD];
OR          : [oO][rR];
ORDER       : [oO][rR][dD][eE][rR];
GROUP       : [gG][rR][oO][uU][pP];
BY          : [bB][yY];
HAVING      : [hH][aA][vV][iI][nN][gG];
UNION       : [uU][nN][iI][oO][nN];
INTERSECT   : [iI][nN][tT][eE][rR][sS][eE][cC][tT];
EXCEPT      : [eE][xX][cC][eE][pP][tT];
LIMIT       : [lL][iI][mM][iI][tT];
OFFSET      : [oO][fF][fF][sS][eE][tT];
FETCH       : [fF][eE][tT][cC][hH];
FIRST       : [fF][iI][rR][sS][tT];
NEXT        : [nN][eE][xX][tT];
ROWS        : [rR][oO][wW][sS];
ROW         : [rR][oO][wW];
ONLY        : [oO][nN][lL][yY];
WITH        : [wW][iI][tT][hH];
TIES        : [tT][iI][eE][sS];
INSERT      : [iI][nN][sS][eE][rR][tT];
UPDATE      : [uU][pP][dD][aA][tT][eE];
DELETE      : [dD][eE][lL][eE][tT][eE];
INTO        : [iI][nN][tT][oO];
VALUES      : [vV][aA][lL][uU][eE][sS];
SET         : [sS][eE][tT];
AS          : [aA][sS];
DISTINCT    : [dD][iI][sS][tT][iI][nN][cC][tT];
ALL         : [aA][lL][lL];
ANY         : [aA][nN][yY];
ASC         : [aA][sS][cC];
DESC        : [dD][eE][sS][cC];
NULLS       : [nN][uU][lL][lL][sS];
CASE        : [cC][aA][sS][eE];
WHEN        : [wW][hH][eE][nN];
THEN        : [tT][hH][eE][nN];
ELSE        : [eE][lL][sS][eE];
END         : [eE][nN][dD];
IN          : [iI][nN];
EXISTS      : [eE][xX][iI][sS][tT][sS];
NOT         : [nN][oO][tT];
NULL        : [nN][uU][lL][lL];
IS          : [iI][sS];
BETWEEN     : [bB][eE][tT][wW][eE][eE][nN];
LIKE        : [lL][iI][kK][eE];
ILIKE       : [iI][lL][iI][kK][eE];
ESCAPE      : [eE][sS][cC][aA][pP][eE];
TRUE        : [tT][rR][uU][eE];
FALSE       : [fF][aA][lL][sS][eE];
CAST        : [cC][aA][sS][tT];
BOTH        : [bB][oO][tT][hH];
LEADING     : [lL][eE][aA][dD][iI][nN][gG];
TRAILING    : [tT][rR][aA][iI][lL][iI][nN][gG];

// Quoted identifiers - preserve exact quoting style
DOUBLE_QUOTED_IDENTIFIER
    : '"' (~["\r\n] | '""')* '"'
    ;

BACKTICK_QUOTED_IDENTIFIER
    : '`' (~[`\r\n])* '`'
    ;

BRACKET_QUOTED_IDENTIFIER
    : '[' ~[\]\r\n]* ']'
    ;

// String literals
STRING_LITERAL
    : '\'' (~'\'' | '\'\'')* '\''
    ;

// Numeric literals
NUMERIC_LITERAL
    : DIGIT+ ('.' DIGIT+)? ([eE] [+-]? DIGIT+)?
    | '.' DIGIT+ ([eE] [+-]? DIGIT+)?
    ;

fragment DIGIT: [0-9];

// Operators and punctuation
LPAREN      : '(';
RPAREN      : ')';
COMMA       : ',';
DOT         : '.';
ASTERISK    : '*';
PLUS        : '+';
MINUS       : '-';
DIVIDE      : '/';
PERCENT     : '%';
EQ          : '=';
NEQ         : ('!=' | '<>');
LT          : '<';
LTE         : '<=';
GT          : '>';
GTE         : '>=';
CONCAT      : '||';
SEMICOLON   : ';';
COLON       : ':';
DOUBLE_COLON: '::';

// Unquoted identifiers (must come after keywords)
IDENTIFIER
    : [a-zA-Z_] [a-zA-Z0-9_$#@]*
    ;

// Whitespace - we need to preserve it for formatting context
WS
    : [ \t\r\n]+
    ;

// Catch-all for any other characters
OTHER
    : .
    ;
