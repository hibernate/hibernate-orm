parser grammar HqlParser;

options {
	tokenVocab=HqlLexer;
}

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.grammars.hql;
}

@members {
	protected void logUseOfReservedWordAsIdentifier(Token token) {
	}
}


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Statements

statement
	: ( selectStatement | updateStatement | deleteStatement | insertStatement ) EOF
	;

selectStatement
	: queryExpression
	;

subQuery
	: queryExpression
	;

dmlTarget
	: entityName identificationVariableDef?
	;

deleteStatement
	: DELETE FROM? dmlTarget whereClause?
	;

updateStatement
	: UPDATE VERSIONED? dmlTarget setClause whereClause?
	;

setClause
	: SET assignment (COMMA assignment)*
	;

assignment
	: dotIdentifierSequence EQUAL expression
	;

insertStatement
	: INSERT INTO? dmlTarget targetFieldsSpec (queryExpression | valuesList)
	;

targetFieldsSpec
	: LEFT_PAREN dotIdentifierSequence (COMMA dotIdentifierSequence)* RIGHT_PAREN
	;

valuesList
	: VALUES values (COMMA values)*
	;

values
	: LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// QUERY SPEC - general structure of root sqm or sub sqm

queryExpression
	: simpleQueryExpression										# SimpleQueryGroup
	| queryExpression (setOperator simpleQueryExpression)+		# SetQueryGroup
	;

simpleQueryExpression
	: querySpec queryOrder?									# QuerySpecExpression
	| LEFT_PAREN queryExpression RIGHT_PAREN queryOrder?	# NestedQueryExpression
	;

setOperator
	: UNION ALL?
	| INTERSECT ALL?
	| EXCEPT ALL?
	;

queryOrder
	:  orderByClause limitClause? offsetClause? fetchClause?
	;

querySpec
// TODO: add with clause
	: selectClause fromClause? whereClause? ( groupByClause havingClause? )?
	| fromClause whereClause? ( groupByClause havingClause? )? selectClause?
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// FROM clause

fromClause
	: FROM fromClauseSpace (COMMA fromClauseSpace)*
	;

fromClauseSpace
	:	pathRoot ( crossJoin | jpaCollectionJoin | qualifiedJoin )*
	;

pathRoot
	: entityName identificationVariableDef?
	;

/**
 * Specialized dotIdentifierSequence for cases where we expect an entity-name.  We handle it specially
 * for the sake of performance.  Specifically we concatenate together the entity name as we walk the
 * parse tree.  Relying on the `EntiytNameContext#getText` or `DotIdentifierSequenceContext#getText`
 * performs walk to determine the name.
 */
entityName
	returns [String fullNameText]
	: (i=identifier { $fullNameText = _localctx.i.getText(); }) (DOT c=identifier { $fullNameText += ("." + _localctx.c.getText() ); })*
	;

identificationVariableDef
	: (AS identifier)
	| IDENTIFIER
	;

crossJoin
	: CROSS JOIN pathRoot identificationVariableDef?
	;

jpaCollectionJoin
	:	COMMA IN LEFT_PAREN path RIGHT_PAREN identificationVariableDef?
	;

qualifiedJoin
	: joinTypeQualifier JOIN FETCH? qualifiedJoinRhs qualifiedJoinPredicate?
	;

joinTypeQualifier
	: INNER?
	| (LEFT|RIGHT|FULL)? OUTER?
	;

qualifiedJoinRhs
	: path identificationVariableDef?
	;

qualifiedJoinPredicate
	: (ON | WITH) predicate
	;



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// SELECT clause

selectClause
	:	SELECT DISTINCT? selectionList
	;

selectionList
	: selection (COMMA selection)*
	;

selection
	: selectExpression resultIdentifier?
	;

selectExpression
	:	dynamicInstantiation
	|	jpaSelectObjectSyntax
	|	mapEntrySelection
	|	expression
	;

resultIdentifier
	: (AS identifier)
	| IDENTIFIER
	;


mapEntrySelection
	: ENTRY LEFT_PAREN path RIGHT_PAREN
	;

dynamicInstantiation
	: NEW dynamicInstantiationTarget LEFT_PAREN dynamicInstantiationArgs RIGHT_PAREN
	;

dynamicInstantiationTarget
	: LIST
	| MAP
	| dotIdentifierSequence
	;

dynamicInstantiationArgs
	:	dynamicInstantiationArg ( COMMA dynamicInstantiationArg )*
	;

dynamicInstantiationArg
	:	dynamicInstantiationArgExpression (AS? identifier)?
	;

dynamicInstantiationArgExpression
	:	expression
	|	dynamicInstantiation
	;

jpaSelectObjectSyntax
	:	OBJECT LEFT_PAREN identifier RIGHT_PAREN
	;




// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Path structures

dotIdentifierSequence
	: identifier dotIdentifierSequenceContinuation*
	;

dotIdentifierSequenceContinuation
	: DOT identifier
	;


/**
 * A path which needs to be resolved semantically.  This recognizes
 * any path-like structure.  Generally, the path is semantically
 * interpreted by the consumer of the parse-tree.  However, there
 * are certain cases where we can syntactically recognize a navigable
 * path; see `syntacticNavigablePath` rule
 */
path
	: syntacticDomainPath (pathContinuation)?
	| generalPathFragment
	;

pathContinuation
	: DOT dotIdentifierSequence
	;

/**
 * Rule for cases where we syntactically know that the path is a
 * "domain path" because it is one of these special cases:
 *
 * 		* TREAT( path )
 * 		* ELEMENTS( path )
 *		* VALUE( path )
 * 		* KEY( path )
 * 		* path[ selector ]
 */
syntacticDomainPath
	: treatedNavigablePath
	| collectionElementNavigablePath
	| mapKeyNavigablePath
	| dotIdentifierSequence indexedPathAccessFragment
	;

/**
 * The main path rule.  Recognition for all normal path structures including
 * class, field and enum references as well as navigable paths.
 *
 * NOTE : this rule does *not* cover the special syntactic navigable path
 * cases: TREAT, KEY, ELEMENTS, VALUES
 */
generalPathFragment
	: dotIdentifierSequence indexedPathAccessFragment?
	;

indexedPathAccessFragment
	: LEFT_BRACKET expression RIGHT_BRACKET (DOT generalPathFragment)?
	;

treatedNavigablePath
	: TREAT LEFT_PAREN path AS dotIdentifierSequence RIGHT_PAREN pathContinuation?
	;

collectionElementNavigablePath
	: (VALUE | ELEMENTS) LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

mapKeyNavigablePath
	: KEY LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// GROUP BY clause

groupByClause
	:	GROUP BY groupByExpression ( COMMA groupByExpression )*
	;

groupByExpression
	: identifier collationSpecification?
	| INTEGER_LITERAL collationSpecification?
	| expression
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//HAVING clause

havingClause
	:	HAVING predicate
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// ORDER BY clause

orderByClause
	: ORDER BY sortSpecification (COMMA sortSpecification)*
	;

/**
 * Specialized rule for ordered Map and Set `@OrderBy` handling
 */
orderByFragment
	: sortSpecification (COMMA sortSpecification)*
	;

sortSpecification
	: sortExpression orderingSpecification? nullsPrecedence?
	;

nullsPrecedence
	: NULLS (FIRST | LAST)
	;

sortExpression
	: identifier collationSpecification?
	| INTEGER_LITERAL collationSpecification?
	| expression
	;

collationSpecification
	:	COLLATE collateName
	;

collateName
	:	dotIdentifierSequence
	;

orderingSpecification
	:	ASC
	|	DESC
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// LIMIT/OFFSET clause

limitClause
	: LIMIT parameterOrIntegerLiteral
	;

offsetClause
	: OFFSET parameterOrIntegerLiteral (ROW | ROWS)?
	;

fetchClause
	: FETCH (FIRST | NEXT) (parameterOrIntegerLiteral | parameterOrNumberLiteral PERCENT) (ROW | ROWS) (ONLY | WITH TIES)
	;

parameterOrIntegerLiteral
	: parameter
	| INTEGER_LITERAL
	;

parameterOrNumberLiteral
	: parameter
	| INTEGER_LITERAL
	| FLOAT_LITERAL
	| DOUBLE_LITERAL
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// WHERE clause & Predicates

whereClause
	:	WHERE predicate
	;

predicate
	//highest to lowest precedence
	: LEFT_PAREN predicate RIGHT_PAREN						# GroupedPredicate
	| expression IS (NOT)? NULL								# IsNullPredicate
	| expression IS (NOT)? EMPTY							# IsEmptyPredicate
	| expression (NOT)? IN inList							# InPredicate
	| expression (NOT)? BETWEEN expression AND expression	# BetweenPredicate
	| expression (NOT)? LIKE expression (likeEscape)?		# LikePredicate
	| expression comparisonOperator expression				# ComparisonPredicate
	| EXISTS expression										# ExistsPredicate
	| expression (NOT)? MEMBER OF path						# MemberOfPredicate
	| NOT predicate											# NegatedPredicate
	| predicate AND predicate								# AndPredicate
	| predicate OR predicate								# OrPredicate
	| expression											# BooleanExpressionPredicate
	;

comparisonOperator
	: EQUAL
	| NOT_EQUAL
	| GREATER
	| GREATER_EQUAL
	| LESS
	| LESS_EQUAL
	| IS DISTINCT FROM
	| IS NOT DISTINCT FROM
	;

inList
	: ELEMENTS? LEFT_PAREN dotIdentifierSequence RIGHT_PAREN		# PersistentCollectionReferenceInList
	| LEFT_PAREN expression (COMMA expression)*	RIGHT_PAREN			# ExplicitTupleInList
	| LEFT_PAREN subQuery RIGHT_PAREN								# SubQueryInList
	| parameter 													# ParamInList
	;

likeEscape
	: ESCAPE expression
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Expression

expression
	//highest to lowest precedence
	: LEFT_PAREN expression RIGHT_PAREN						# GroupedExpression
	| LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN	# TupleExpression
	| LEFT_PAREN subQuery RIGHT_PAREN						# SubQueryExpression
	| primaryExpression collationSpecification?				# CollateExpression
	| signOperator expression								# UnaryExpression
	| expression datetimeField  							# ToDurationExpression
	| expression BY datetimeField							# FromDurationExpression
	| expression multiplicativeOperator expression			# MultiplicationExpression
	| expression additiveOperator expression				# AdditionExpression
	| expression DOUBLE_PIPE expression						# ConcatenationExpression
	;

primaryExpression
	: caseList											# CaseExpression
	| literal											# LiteralExpression
	| parameter											# ParameterExpression
	| entityTypeReference								# EntityTypeExpression
	| entityIdReference									# EntityIdExpression
	| entityVersionReference							# EntityVersionExpression
	| entityNaturalIdReference							# EntityNaturalIdExpression
	| path												# PathExpression
	| function											# FunctionExpression
	;

multiplicativeOperator
	: SLASH
	| PERCENT_OP
	| ASTERISK
	;

additiveOperator
	: PLUS
	| MINUS
	;

signOperator
	: PLUS
	| MINUS
	;

entityTypeReference
	: TYPE LEFT_PAREN (path | parameter) RIGHT_PAREN
	;

entityIdReference
	: ID LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

entityVersionReference
	: VERSION LEFT_PAREN path RIGHT_PAREN
	;

entityNaturalIdReference
	: NATURALID LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

caseList
	: simpleCaseList
	| searchedCaseList
	;

simpleCaseList
	: CASE expression (simpleCaseWhen)+ (caseOtherwise)? END
	;

simpleCaseWhen
	: WHEN expression THEN expression
	;

caseOtherwise
	: ELSE expression
	;

searchedCaseList
	: CASE (searchedCaseWhen)+ (caseOtherwise)? END
	;

searchedCaseWhen
	: WHEN predicate THEN expression
	;

greatestFunction
	: GREATEST LEFT_PAREN expression (COMMA expression)+ RIGHT_PAREN
	;

leastFunction
	: LEAST LEFT_PAREN expression (COMMA expression)+ RIGHT_PAREN
	;

coalesceFunction
	: COALESCE LEFT_PAREN expression (COMMA expression)+ RIGHT_PAREN
	;

ifnullFunction
	: IFNULL LEFT_PAREN expression COMMA expression RIGHT_PAREN
	;

nullifFunction
	: NULLIF LEFT_PAREN expression COMMA expression RIGHT_PAREN
	;

literal
	: STRING_LITERAL
	| INTEGER_LITERAL
	| LONG_LITERAL
	| BIG_INTEGER_LITERAL
	| FLOAT_LITERAL
	| DOUBLE_LITERAL
	| BIG_DECIMAL_LITERAL
	| HEX_LITERAL
	| NULL
	| TRUE
	| FALSE
	| binaryLiteral
	| temporalLiteral
	| generalizedLiteral
	;

binaryLiteral
	: BINARY_LITERAL
	| LEFT_BRACE HEX_LITERAL (COMMA HEX_LITERAL)* RIGHT_BRACE
	;

temporalLiteral
	: dateTimeLiteral
	| dateLiteral
	| timeLiteral
	| jdbcTimestampLiteral
	| jdbcDateLiteral
    | jdbcTimeLiteral
	;

dateTimeLiteral
	: LEFT_BRACE dateTime RIGHT_BRACE
	| DATETIME dateTime
	;

dateLiteral
	: LEFT_BRACE date RIGHT_BRACE
	| DATE date
	;

timeLiteral
	: LEFT_BRACE time RIGHT_BRACE
	| TIME time
	;

dateTime
	: date time (zoneId | offset)?
	;

date
	: year MINUS month MINUS day
	;

time
	: hour COLON minute (COLON second)?
	;

offset
	: (PLUS | MINUS) hour (COLON minute)?
	;

year: INTEGER_LITERAL;
month: INTEGER_LITERAL;
day: INTEGER_LITERAL;
hour: INTEGER_LITERAL;
minute: INTEGER_LITERAL;
second: INTEGER_LITERAL | FLOAT_LITERAL;
zoneId
	: IDENTIFIER (SLASH IDENTIFIER)?
	| STRING_LITERAL;

jdbcTimestampLiteral
	: TIMESTAMP_ESCAPE_START (dateTime | genericTemporalLiteralText) RIGHT_BRACE
	;

jdbcDateLiteral
	: DATE_ESCAPE_START (date | genericTemporalLiteralText) RIGHT_BRACE
	;

jdbcTimeLiteral
	: TIME_ESCAPE_START (time | genericTemporalLiteralText) RIGHT_BRACE
	;

genericTemporalLiteralText
	: STRING_LITERAL
	;

generalizedLiteral
	: LEFT_BRACE generalizedLiteralType COLON generalizedLiteralText RIGHT_BRACE
	;

generalizedLiteralType : STRING_LITERAL;
generalizedLiteralText : STRING_LITERAL;


parameter
	: COLON identifier					# NamedParameter
	| QUESTION_MARK INTEGER_LITERAL?	# PositionalParameter
	;

function
	: standardFunction
	| aggregateFunction
	| jpaCollectionFunction
	| hqlCollectionFunction
	| jpaNonStandardFunction
	| nonStandardFunction
	;

jpaNonStandardFunction
	: FUNCTION LEFT_PAREN jpaNonStandardFunctionName (COMMA nonStandardFunctionArguments)? RIGHT_PAREN
	;

jpaNonStandardFunctionName
	: STRING_LITERAL
	;

nonStandardFunction
	: nonStandardFunctionName LEFT_PAREN nonStandardFunctionArguments? RIGHT_PAREN
	;

nonStandardFunctionName
	: dotIdentifierSequence
	;

nonStandardFunctionArguments
	: (datetimeField COMMA)? expression (COMMA expression)*
	;

jpaCollectionFunction
	: SIZE LEFT_PAREN path RIGHT_PAREN					# CollectionSizeFunction
	| INDEX LEFT_PAREN identifier RIGHT_PAREN			# CollectionIndexFunction
	;

hqlCollectionFunction
	: MAXINDEX LEFT_PAREN path RIGHT_PAREN				# MaxIndexFunction
	| MAXELEMENT LEFT_PAREN path RIGHT_PAREN			# MaxElementFunction
	| MININDEX LEFT_PAREN path RIGHT_PAREN				# MinIndexFunction
	| MINELEMENT LEFT_PAREN path RIGHT_PAREN			# MinElementFunction
	;

aggregateFunction
	: avgFunction
	| sumFunction
	| minFunction
	| maxFunction
	| countFunction
	| everyFunction
	| anyFunction
	;

avgFunction
	: AVG LEFT_PAREN DISTINCT? expression RIGHT_PAREN filterClause?
	;

sumFunction
	: SUM LEFT_PAREN DISTINCT? expression RIGHT_PAREN filterClause?
	;

minFunction
	: MIN LEFT_PAREN DISTINCT? expression RIGHT_PAREN filterClause?
	;

maxFunction
	: MAX LEFT_PAREN DISTINCT? expression RIGHT_PAREN filterClause?
	;

countFunction
	: COUNT LEFT_PAREN DISTINCT? (expression | ASTERISK) RIGHT_PAREN filterClause?
	;

everyFunction
	: (EVERY|ALL) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (EVERY|ALL) LEFT_PAREN subQuery RIGHT_PAREN
	;

anyFunction
	: (ANY|SOME) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (ANY|SOME) LEFT_PAREN subQuery RIGHT_PAREN
	;

filterClause
	: FILTER LEFT_PAREN whereClause RIGHT_PAREN
	;

standardFunction
	:	castFunction
	|	extractFunction
	|	coalesceFunction
	|	nullifFunction
	|	ifnullFunction
	|	formatFunction
	|	concatFunction
	|	substringFunction
	|	leftFunction
	|	rightFunction
	|	overlayFunction
	|	replaceFunction
	|	trimFunction
	|	padFunction
	|	upperFunction
	|	lowerFunction
	|	locateFunction
	|	positionFunction
	|	lengthFunction
	|	absFunction
	|	signFunction
	|	sqrtFunction
	|	lnFunction
	|	expFunction
	|	modFunction
	|	powerFunction
	|	ceilingFunction
	|	floorFunction
	|	roundFunction
	|	trigFunction
	|	atan2Function
	|	strFunction
	|	greatestFunction
	|	leastFunction
	|	currentDateFunction
	|	currentTimeFunction
	|	currentTimestampFunction
	|	instantFunction
	|	localDateFunction
	|	localTimeFunction
	|	localDateTimeFunction
	|	offsetDateTimeFunction
	|	cube
	|	rollup
	;


castFunction
	: CAST LEFT_PAREN expression AS castTarget RIGHT_PAREN
	;

castTarget
	: castTargetType (LEFT_PAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RIGHT_PAREN)?
	;

/**
 * Like the `entityName` rule, we have a specialized dotIdentifierSequence rule
 */
castTargetType
	returns [String fullTargetName]
	: (i=identifier { $fullTargetName = _localctx.i.getText(); }) (DOT c=identifier { $fullTargetName += ("." + _localctx.c.getText() ); })*
	;

concatFunction
	: CONCAT LEFT_PAREN expression (COMMA expression)+ RIGHT_PAREN
	;

leftFunction
	: LEFT LEFT_PAREN expression COMMA expression RIGHT_PAREN
	;
rightFunction
	: RIGHT LEFT_PAREN expression COMMA expression RIGHT_PAREN
	;

substringFunction
	: SUBSTRING LEFT_PAREN expression COMMA substringFunctionStartArgument (COMMA substringFunctionLengthArgument)? RIGHT_PAREN
	| SUBSTRING LEFT_PAREN expression FROM substringFunctionStartArgument (FOR substringFunctionLengthArgument)? RIGHT_PAREN
	;

substringFunctionStartArgument
	: expression
	;

substringFunctionLengthArgument
	: expression
	;

trimFunction
	: TRIM LEFT_PAREN trimSpecification? trimCharacter? FROM? expression RIGHT_PAREN
	;

trimSpecification
	: LEADING
	| TRAILING
	| BOTH
	;

trimCharacter
	: STRING_LITERAL
	;

padFunction
	: PAD LEFT_PAREN expression WITH padLength padSpecification padCharacter? RIGHT_PAREN
	;

padSpecification
	: LEADING
	| TRAILING
	;

padCharacter
	: STRING_LITERAL
	;

padLength
	: expression
	;

upperFunction
	: UPPER LEFT_PAREN expression RIGHT_PAREN
	;

lowerFunction
	: LOWER LEFT_PAREN expression RIGHT_PAREN
	;

locateFunction
	: LOCATE LEFT_PAREN locateFunctionPatternArgument COMMA locateFunctionStringArgument (COMMA locateFunctionStartArgument)? RIGHT_PAREN
	;

locateFunctionPatternArgument
	: expression
	;

locateFunctionStringArgument
	: expression
	;

locateFunctionStartArgument
	: expression
	;

overlayFunction
	: OVERLAY LEFT_PAREN overlayFunctionStringArgument PLACING overlayFunctionReplacementArgument FROM overlayFunctionStartArgument (FOR overlayFunctionLengthArgument)? RIGHT_PAREN
	;

overlayFunctionStringArgument
	: expression
	;

overlayFunctionReplacementArgument
	: expression
	;

overlayFunctionStartArgument
	: expression
	;

overlayFunctionLengthArgument
	: expression
	;

replaceFunction
	: REPLACE LEFT_PAREN replaceFunctionStringArgument COMMA replaceFunctionPatternArgument COMMA replaceFunctionReplacementArgument RIGHT_PAREN
	;

replaceFunctionStringArgument
	: expression
	;

replaceFunctionPatternArgument
	: expression
	;

replaceFunctionReplacementArgument
	: expression
	;

lengthFunction
	:	LENGTH LEFT_PAREN expression RIGHT_PAREN
	;

absFunction
	:	ABS LEFT_PAREN expression RIGHT_PAREN
	;

signFunction
	:	SIGN LEFT_PAREN expression RIGHT_PAREN
	;

sqrtFunction
	:	SQRT LEFT_PAREN expression RIGHT_PAREN
	;

lnFunction
	:	LN LEFT_PAREN expression RIGHT_PAREN
	;

expFunction
	:	EXP LEFT_PAREN expression RIGHT_PAREN
	;

powerFunction
	:	POWER LEFT_PAREN powerBaseArgument COMMA powerPowerArgument RIGHT_PAREN
	;

powerBaseArgument
	: expression
	;

powerPowerArgument
	: expression
	;

modFunction
	:	MOD LEFT_PAREN modDividendArgument COMMA modDivisorArgument RIGHT_PAREN
	;

modDividendArgument
	: expression
	;

modDivisorArgument
	: expression
	;

ceilingFunction
	:	CEILING LEFT_PAREN expression RIGHT_PAREN
	;

floorFunction
	:	FLOOR LEFT_PAREN expression RIGHT_PAREN
	;

roundFunction
	:	ROUND LEFT_PAREN expression COMMA roundFunctionPrecision RIGHT_PAREN
	;

roundFunctionPrecision
	: expression
	;

trigFunction
	:	trigFunctionName LEFT_PAREN expression RIGHT_PAREN
	;

trigFunctionName
    : COS
    | SIN
    | TAN
    | ACOS
    | ASIN
    | ATAN
    //ATAN2 is different!
    ;

atan2Function
	:	ATAN2 LEFT_PAREN expression COMMA expression RIGHT_PAREN
	;

strFunction
	:   STR LEFT_PAREN expression RIGHT_PAREN
	;

currentDateFunction
	: CURRENT_DATE (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT DATE
	;

currentTimeFunction
	: CURRENT_TIME (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT TIME
	;

currentTimestampFunction
	: CURRENT_TIMESTAMP (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT TIMESTAMP
	;

instantFunction
	: CURRENT_INSTANT (LEFT_PAREN RIGHT_PAREN)? //deprecated legacy syntax
	| INSTANT
	;

localDateTimeFunction
	: LOCAL_DATETIME (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL DATETIME
	;

offsetDateTimeFunction
	: OFFSET_DATETIME (LEFT_PAREN RIGHT_PAREN)?
	| OFFSET DATETIME
	;

localDateFunction
	: LOCAL_DATE (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL DATE
	;

localTimeFunction
	: LOCAL_TIME (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL TIME
	;

formatFunction
	: FORMAT LEFT_PAREN expression AS format RIGHT_PAREN
	;

format
	: STRING_LITERAL
	;

extractFunction
	: EXTRACT LEFT_PAREN extractField FROM expression RIGHT_PAREN
	| datetimeField LEFT_PAREN expression RIGHT_PAREN
	;

extractField
	: datetimeField
	| dayField
	| weekField
	| timeZoneField
	| dateOrTimeField
	;

datetimeField
	: YEAR
	| MONTH
	| DAY
	| WEEK
	| QUARTER
	| HOUR
	| MINUTE
	| SECOND
	| NANOSECOND
	;

dayField
	: DAY OF MONTH
	| DAY OF WEEK
	| DAY OF YEAR
	;

weekField
	: WEEK OF MONTH
	| WEEK OF YEAR
	;

timeZoneField
	: OFFSET (HOUR | MINUTE)?
	| TIMEZONE_HOUR | TIMEZONE_MINUTE
	;

dateOrTimeField
	: DATE
	| TIME
	;

positionFunction
	: POSITION LEFT_PAREN positionFunctionPatternArgument IN positionFunctionStringArgument RIGHT_PAREN
	;

positionFunctionPatternArgument
	: expression
	;

positionFunctionStringArgument
	: expression
	;

cube
	: CUBE LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN
	;

rollup
	: ROLLUP LEFT_PAREN expression (COMMA expression)* RIGHT_PAREN
	;

/**
 * The `identifier` is used to provide "keyword as identifier" handling.
 *
 * The lexer hands us recognized keywords using their specific tokens.  This is important
 * for the recognition of sqm structure, especially in terms of performance!
 *
 * However we want to continue to allow users to use most keywords as identifiers (e.g., attribute names).
 * This parser rule helps with that.  Here we expect that the caller already understands their
 * context enough to know that keywords-as-identifiers are allowed.
 */
identifier
	: IDENTIFIER
	| (ABS
	| ALL
	| AND
	| ANY
	| AS
	| ASC
	| ATAN2
	| AVG
	| BY
	| BETWEEN
	| BOTH
	| CASE
	| CAST
	| CEILING
	| CLASS
	| COALESCE
	| COLLATE
	| CONCAT
	| COUNT
	| CROSS
	| CURRENT_DATE
	| CURRENT_INSTANT
	| CURRENT_TIME
	| CURRENT_TIMESTAMP
	| DAY
	| DATE
	| DAY
	| DELETE
	| DESC
	| DISTINCT
	| ELEMENTS
	| ELSE
	| EMPTY
	| END
	| ENTRY
	| EVERY
	| ESCAPE
	| EXISTS
	| EXP
	| EXTRACT
	| FETCH
	| FILTER
	| FLOOR
	| FROM
	| FOR
	| FORMAT
	| FULL
	| FUNCTION
	| GREATEST
	| GROUP
	| HOUR
	| ID
	| IFNULL
	| IN
	| INDEX
	| INNER
	| INSERT
	| INSTANT
	| INTO
	| IS
	| JOIN
	| KEY
	| LEADING
	| LEAST
	| LEFT
	| LENGTH
	| LIKE
	| LIMIT
	| LIST
	| LN
	| LOCATE
	| LOWER
	| MAP
	| MAX
	| MAXELEMENT
	| MAXINDEX
	| MEMBER
	| MICROSECOND
	| MILLISECOND
	| MIN
	| MINELEMENT
	| MININDEX
	| MINUTE
	| MEMBER
	| MOD
	| MONTH
	| NANOSECOND
	| NATURALID
	| NEW
	| NOT
	| NULLIF
	| OBJECT
	| OF
	| ON
	| OR
	| ORDER
	| OUTER
	| PAD
	| POSITION
	| POWER
	| QUARTER
	| REPLACE
	| RIGHT
	| ROUND
	| RIGHT
	| SECOND
	| SELECT
	| SET
	| SIGN
	| SIZE
	| SOME
	| SQRT
	| STR
	| SUBSTRING
	| SUM
	| THEN
	| TIME
	| TIMESTAMP
	| TIMEZONE_HOUR
	| TIMEZONE_MINUTE
	| TRAILING
	| TREAT
	| TRIM
	| TYPE
	| UPDATE
	| UPPER
	| VALUE
	| VERSION
	| VERSIONED
	| WEEK
	| WHERE
	| WITH
	| YEAR
	| trigFunctionName) {
		logUseOfReservedWordAsIdentifier( getCurrentToken() );
	}
	;

