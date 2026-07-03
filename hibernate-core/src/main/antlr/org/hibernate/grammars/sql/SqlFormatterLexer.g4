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

// Keywords (alphabetically ordered)
ALL         : [aA][lL][lL];
ALTER       : [aA][lL][tT][eE][rR];
ALWAYS      : [aA][lL][wW][aA][yY][sS];
AND         : [aA][nN][dD];
ANY         : [aA][nN][yY];
AS          : [aA][sS];
ASC         : [aA][sS][cC];
BETWEEN     : [bB][eE][tT][wW][eE][eE][nN];
BOTH        : [bB][oO][tT][hH];
BY          : [bB][yY];
CACHE       : [cC][aA][cC][hH][eE];
CASCADE     : [cC][aA][sS][cC][aA][dD][eE];
CASE        : [cC][aA][sS][eE];
CAST        : [cC][aA][sS][tT];
CHECK       : [cC][hH][eE][cC][kK];
CONFLICT    : [cC][oO][nN][fF][lL][iI][cC][tT];
CONSTRAINT  : [cC][oO][nN][sS][tT][rR][aA][iI][nN][tT];
CONTINUE    : [cC][oO][nN][tT][iI][nN][uU][eE];
CREATE      : [cC][rR][eE][aA][tT][eE];
CROSS       : [cC][rR][oO][sS][sS];
CURRENT     : [cC][uU][rR][rR][eE][nN][tT];
CYCLE       : [cC][yY][cC][lL][eE];
DEFAULT     : [dD][eE][fF][aA][uU][lL][tT];
DELETE      : [dD][eE][lL][eE][tT][eE];
DESC        : [dD][eE][sS][cC];
DISTINCT    : [dD][iI][sS][tT][iI][nN][cC][tT];
DO          : [dD][oO];
DROP        : [dD][rR][oO][pP];
DUPLICATE   : [dD][uU][pP][lL][iI][cC][aA][tT][eE];
ELSE        : [eE][lL][sS][eE];
END         : [eE][nN][dD];
ESCAPE      : [eE][sS][cC][aA][pP][eE];
EXCEPT      : [eE][xX][cC][eE][pP][tT];
EXCLUDE     : [eE][xX][cC][lL][uU][dD][eE];
EXISTS      : [eE][xX][iI][sS][tT][sS];
FALSE       : [fF][aA][lL][sS][eE];
FETCH       : [fF][eE][tT][cC][hH];
FIRST       : [fF][iI][rR][sS][tT];
FOLLOWING   : [fF][oO][lL][lL][oO][wW][iI][nN][gG];
FOR         : [fF][oO][rR];
FOREIGN     : [fF][oO][rR][eE][iI][gG][nN];
FROM        : [fF][rR][oO][mM];
FULL        : [fF][uU][lL][lL];
GENERATED   : [gG][eE][nN][eE][rR][aA][tT][eE][dD];
GLOBAL      : [gG][lL][oO][bB][aA][lL];
GROUP       : [gG][rR][oO][uU][pP];
GROUPS      : [gG][rR][oO][uU][pP][sS];
HAVING      : [hH][aA][vV][iI][nN][gG];
IDENTITY    : [iI][dD][eE][nN][tT][iI][tT][yY];
ILIKE       : [iI][lL][iI][kK][eE];
IN          : [iI][nN];
INCREMENT   : [iI][nN][cC][rR][eE][mM][eE][nN][tT];
INDEX       : [iI][nN][dD][eE][xX];
INNER       : [iI][nN][nN][eE][rR];
INSERT      : [iI][nN][sS][eE][rR][tT];
INTERSECT   : [iI][nN][tT][eE][rR][sS][eE][cC][tT];
INTERVAL    : [iI][nN][tT][eE][rR][vV][aA][lL];
INTO        : [iI][nN][tT][oO];
IS          : [iI][sS];
JOIN        : [jJ][oO][iI][nN];
KEY         : [kK][eE][yY];
LAST        : [lL][aA][sS][tT];
LATERAL     : [lL][aA][tT][eE][rR][aA][lL];
LEADING     : [lL][eE][aA][dD][iI][nN][gG];
LEFT        : [lL][eE][fF][tT];
LIKE        : [lL][iI][kK][eE];
LIMIT       : [lL][iI][mM][iI][tT];
MATCHED     : [mM][aA][tT][cC][hH][eE][dD];
MATERIALIZED: [mM][aA][tT][eE][rR][iI][aA][lL][iI][zZ][eE][dD];
MAXVALUE    : [mM][aA][xX][vV][aA][lL][uU][eE];
MERGE       : [mM][eE][rR][gG][eE];
MINVALUE    : [mM][iI][nN][vV][aA][lL][uU][eE];
NEXT        : [nN][eE][xX][tT];
NO          : [nN][oO];
NOT         : [nN][oO][tT];
NOTHING     : [nN][oO][tT][hH][iI][nN][gG];
NULL        : [nN][uU][lL][lL];
NULLS       : [nN][uU][lL][lL][sS];
OF          : [oO][fF];
OFFSET      : [oO][fF][fF][sS][eE][tT];
ON          : [oO][nN];
ONLY        : [oO][nN][lL][yY];
OR          : [oO][rR];
ORDER       : [oO][rR][dD][eE][rR];
OTHERS      : [oO][tT][hH][eE][rR][sS];
OUTER       : [oO][uU][tT][eE][rR];
OVER        : [oO][vV][eE][rR];
PARTITION   : [pP][aA][rR][tT][iI][tT][iI][oO][nN];
PRECEDING   : [pP][rR][eE][cC][eE][dD][iI][nN][gG];
PRIMARY     : [pP][rR][iI][mM][aA][rR][yY];
RANGE       : [rR][aA][nN][gG][eE];
RECURSIVE   : [rR][eE][cC][uU][rR][sS][iI][vV][eE];
REFERENCES  : [rR][eE][fF][eE][rR][eE][nN][cC][eE][sS];
REFRESH     : [rR][eE][fF][rR][eE][sS][hH];
REPLACE     : [rR][eE][pP][lL][aA][cC][eE];
RESTART     : [rR][eE][sS][tT][aA][rR][tT];
RETURNING   : [rR][eE][tT][uU][rR][nN][iI][nN][gG];
RIGHT       : [rR][iI][gG][hH][tT];
ROW         : [rR][oO][wW];
ROWS        : [rR][oO][wW][sS];
SELECT      : [sS][eE][lL][eE][cC][tT];
SEQUENCE    : [sS][eE][qQ][uU][eE][nN][cC][eE];
SET         : [sS][eE][tT];
START       : [sS][tT][aA][rR][tT];
TABLE       : [tT][aA][bB][lL][eE];
TEMP        : [tT][eE][mM][pP];
TEMPORARY   : [tT][eE][mM][pP][oO][rR][aA][rR][yY];
THEN        : [tT][hH][eE][nN];
TIES        : [tT][iI][eE][sS];
TO          : [tT][oO];
TRAILING    : [tT][rR][aA][iI][lL][iI][nN][gG];
TRANSACTIONAL: [tT][rR][aA][nN][sS][aA][cC][tT][iI][oO][nN][aA][lL];
TRUE        : [tT][rR][uU][eE];
TRUNCATE    : [tT][rR][uU][nN][cC][aA][tT][eE];
UNBOUNDED   : [uU][nN][bB][oO][uU][nN][dD][eE][dD];
UNION       : [uU][nN][iI][oO][nN];
UNIQUE      : [uU][nN][iI][qQ][uU][eE];
UPDATE      : [uU][pP][dD][aA][tT][eE];
USING       : [uU][sS][iI][nN][gG];
VALUES      : [vV][aA][lL][uU][eE][sS];
VIEW        : [vV][iI][eE][wW];
WHEN        : [wW][hH][eE][nN];
WHERE       : [wW][hH][eE][rR][eE];
WINDOW      : [wW][iI][nN][dD][oO][wW];
WITH        : [wW][iI][tT][hH];

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
    : [ \t\r\n]+ -> skip;

// Catch-all for any other characters
OTHER
    : .
    ;
