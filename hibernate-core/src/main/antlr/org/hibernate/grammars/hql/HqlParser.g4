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

subquery
	: queryExpression
	;

targetEntity
	: entityName identificationVariableDef?
	;

deleteStatement
	: DELETE FROM? targetEntity whereClause?
	;

updateStatement
	: UPDATE VERSIONED? targetEntity setClause whereClause?
	;

setClause
	: SET assignment (COMMA assignment)*
	;

assignment
	: dotIdentifierSequence EQUAL expressionOrPredicate
	;

insertStatement
	: INSERT INTO? targetEntity targetFields (queryExpression | valuesList)
	;

targetFields
	: LEFT_PAREN dotIdentifierSequence (COMMA dotIdentifierSequence)* RIGHT_PAREN
	;

valuesList
	: VALUES values (COMMA values)*
	;

values
	: LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// QUERY SPEC - general structure of root sqm or sub sqm

queryExpression
	: orderedQuery								# SimpleQueryGroup
	| orderedQuery (setOperator orderedQuery)+	# SetQueryGroup
	;

orderedQuery
	: query queryOrder?										# QuerySpecExpression
	| LEFT_PAREN queryExpression RIGHT_PAREN queryOrder?	# NestedQueryExpression
	;

setOperator
	: UNION ALL?
	| INTERSECT ALL?
	| EXCEPT ALL?
	;

queryOrder
	: orderByClause limitClause? offsetClause? fetchClause?
	;

query
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

entityName
	: identifier (DOT identifier)*
	;

identificationVariableDef
	: AS identifier
	| IDENTIFIER
	| QUOTED_IDENTIFIER
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
	: selectExpression identificationVariableDef?
	;

selectExpression
	:	dynamicInstantiation
	|	jpaSelectObjectSyntax
	|	mapEntrySelection
	|	expressionOrPredicate
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
	:	dynamicInstantiationArgExpression identificationVariableDef?
	;

dynamicInstantiationArgExpression
	:	expressionOrPredicate
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
	: syntacticDomainPath pathContinuation?
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
 * 		* INDICES( path )
 *		* VALUE( path )
 * 		* KEY( path )
 * 		* path[ selector ]
 */
syntacticDomainPath
	: treatedNavigablePath
	| collectionElementNavigablePath
	| collectionIndexNavigablePath
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

collectionIndexNavigablePath
	: INDICES LEFT_PAREN path RIGHT_PAREN
	;

mapKeyNavigablePath
	: KEY LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// GROUP BY clause

groupByClause
	: GROUP BY groupByExpression ( COMMA groupByExpression )*
	;

groupByExpression
	: identifier
	| INTEGER_LITERAL
	| expression
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//HAVING clause

havingClause
	: HAVING predicate
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
	: identifier
	| INTEGER_LITERAL
	| expression
	;

collationExpression
	: COLLATE LEFT_PAREN expression AS collation RIGHT_PAREN
	;

collation
	: dotIdentifierSequence
	;

orderingSpecification
	: ASC
	| DESC
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
	: WHERE predicate
	;

predicate
	//highest to lowest precedence
	: LEFT_PAREN predicate RIGHT_PAREN											# GroupedPredicate
	| expression IS NOT? NULL													# IsNullPredicate
	| expression IS NOT? EMPTY													# IsEmptyPredicate
	| expression NOT? IN inList													# InPredicate
	| expression NOT? BETWEEN expression AND expression							# BetweenPredicate
	| expression NOT? (LIKE | ILIKE) expression likeEscape?						# LikePredicate
	| expression comparisonOperator expression									# ComparisonPredicate
	| EXISTS (ELEMENTS|INDICES) LEFT_PAREN dotIdentifierSequence RIGHT_PAREN	# ExistsCollectionPartPredicate
	| EXISTS expression															# ExistsPredicate
	| expression NOT? MEMBER OF? path											# MemberOfPredicate
	| NOT predicate																# NegatedPredicate
	| predicate AND predicate													# AndPredicate
	| predicate OR predicate													# OrPredicate
	| expression																# BooleanExpressionPredicate
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
	: (ELEMENTS|INDICES) LEFT_PAREN dotIdentifierSequence RIGHT_PAREN				# PersistentCollectionReferenceInList
	| LEFT_PAREN (expressionOrPredicate (COMMA expressionOrPredicate)*)? RIGHT_PAREN# ExplicitTupleInList
	| LEFT_PAREN subquery RIGHT_PAREN												# SubqueryInList
	| parameter 																	# ParamInList
	;

likeEscape
	: ESCAPE STRING_LITERAL
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Expression

expression
	//highest to lowest precedence
	: LEFT_PAREN expression RIGHT_PAREN												# GroupedExpression
	| LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)+ RIGHT_PAREN	# TupleExpression
	| LEFT_PAREN subquery RIGHT_PAREN												# SubqueryExpression
	| primaryExpression 															# BarePrimaryExpression
	| signOperator numericLiteral													# UnaryNumericLiteralExpression
	| signOperator expression														# UnaryExpression
	| expression datetimeField  													# ToDurationExpression
	| expression BY datetimeField													# FromDurationExpression
	| expression multiplicativeOperator expression									# MultiplicationExpression
	| expression additiveOperator expression										# AdditionExpression
	| expression DOUBLE_PIPE expression												# ConcatenationExpression
	;

primaryExpression
	: caseList											# CaseExpression
	| literal											# LiteralExpression
	| parameter											# ParameterExpression
	| entityTypeReference								# EntityTypeExpression
	| entityIdReference									# EntityIdExpression
	| entityVersionReference							# EntityVersionExpression
	| entityNaturalIdReference							# EntityNaturalIdExpression
	| syntacticDomainPath pathContinuation?			# SyntacticPathExpression
	| function											# FunctionExpression
	| generalPathFragment								# GeneralPathExpression
	;

expressionOrPredicate
	: expression
	| predicate
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
	: CASE expressionOrPredicate simpleCaseWhen+ caseOtherwise? END
	;

simpleCaseWhen
	: WHEN expression THEN expressionOrPredicate
	;

caseOtherwise
	: ELSE expressionOrPredicate
	;

searchedCaseList
	: CASE searchedCaseWhen+ caseOtherwise? END
	;

searchedCaseWhen
	: WHEN predicate THEN expressionOrPredicate
	;

literal
	: STRING_LITERAL
	| NULL
	| TRUE
	| FALSE
	| numericLiteral
	| binaryLiteral
	| temporalLiteral
	| generalizedLiteral
	;

numericLiteral
	: INTEGER_LITERAL
	| LONG_LITERAL
	| BIG_INTEGER_LITERAL
	| FLOAT_LITERAL
	| DOUBLE_LITERAL
	| BIG_DECIMAL_LITERAL
	| HEX_LITERAL
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
	| genericFunction
	;

jpaNonStandardFunction
	: FUNCTION LEFT_PAREN jpaNonStandardFunctionName (COMMA nonStandardFunctionArguments)? RIGHT_PAREN
	;

jpaNonStandardFunctionName
	: STRING_LITERAL
	;

genericFunction
	: genericFunctionName LEFT_PAREN (nonStandardFunctionArguments | ASTERISK)? RIGHT_PAREN filterClause?
	;

genericFunctionName
	: dotIdentifierSequence
	;

nonStandardFunctionArguments
	: (DISTINCT | datetimeField COMMA)? expressionOrPredicate (COMMA expressionOrPredicate)*
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
	: everyFunction
	| anyFunction
	;

everyFunction
	: (EVERY|ALL) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (EVERY|ALL) LEFT_PAREN subquery RIGHT_PAREN
	| (EVERY|ALL) (ELEMENTS|INDICES) LEFT_PAREN dotIdentifierSequence RIGHT_PAREN
	;

anyFunction
	: (ANY|SOME) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (ANY|SOME) LEFT_PAREN subquery RIGHT_PAREN
	| (ANY|SOME) (ELEMENTS|INDICES) LEFT_PAREN dotIdentifierSequence RIGHT_PAREN
	;

filterClause
	: FILTER LEFT_PAREN whereClause RIGHT_PAREN
	;

standardFunction
	:	castFunction
	|	extractFunction
	|	formatFunction
	|	substringFunction
	|	overlayFunction
	|	trimFunction
	|	padFunction
	|	positionFunction
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
	|	collationExpression
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
	: CUBE LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
	;

rollup
	: ROLLUP LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
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
	| QUOTED_IDENTIFIER
	| (ALL
	| AND
	| ANY
	| AS
	| ASC
	| BETWEEN
	| BOTH
	| BY
	| CASE
	| CAST
	| COLLATE
	| CROSS
	| CUBE
	| CURRENT
	| CURRENT_DATE
	| CURRENT_INSTANT
	| CURRENT_TIME
	| CURRENT_TIMESTAMP
	| DATE
	| DAY
	| DATETIME
	| DELETE
	| DESC
	| DISTINCT
	| ELEMENTS
	| ELSE
	| EMPTY
	| END
	| ENTRY
	| ESCAPE
	| EVERY
	| EXCEPT
	| EXISTS
	| EXTRACT
	| FETCH
	| FILTER
	| FIRST
	| FOR
	| FORMAT
	| FROM
	| FULL
	| FUNCTION
	| GROUP
	| HAVING
	| HOUR
	| ID
	| ILIKE
	| IN
	| INDEX
	| INDICES
	| INNER
	| INSERT
	| INSTANT
	| INTERSECT
	| INTO
	| IS
	| JOIN
	| KEY
	| LAST
	| LEADING
	| LEFT
	| LIKE
	| LIMIT
	| LIST
	| LOCAL
	| LOCAL_DATE
	| LOCAL_DATETIME
	| LOCAL_TIME
	| MAP
	| MAXELEMENT
	| MAXINDEX
	| MEMBER
	| MICROSECOND
	| MILLISECOND
	| MINELEMENT
	| MININDEX
	| MINUTE
	| MONTH
	| NANOSECOND
	| NATURALID
	| NEW
	| NEXT
	| NOT
	| NULLS
	| OBJECT
	| OF
	| OFFSET
	| OFFSET_DATETIME
	| ON
	| ONLY
	| OR
	| ORDER
	| OUTER
	| OVERLAY
	| PAD
	| PERCENT
	| PLACING
	| POSITION
	| QUARTER
	| RIGHT
	| ROLLUP
	| ROW
	| ROWS
	| SECOND
	| SELECT
	| SET
	| SIZE
	| SOME
	| SUBSTRING
	| THEN
	| TIES
	| TIME
	| TIMESTAMP
	| TIMEZONE_HOUR
	| TIMEZONE_MINUTE
	| TRAILING
	| TREAT
	| TRIM
	| TYPE
	| UNION
	| UPDATE
	| VALUE
	| VALUES
	| VERSION
	| VERSIONED
	| WEEK
	| WHEN
	| WHERE
	| WITH
	| YEAR) {
		logUseOfReservedWordAsIdentifier( getCurrentToken() );
	}
	;
