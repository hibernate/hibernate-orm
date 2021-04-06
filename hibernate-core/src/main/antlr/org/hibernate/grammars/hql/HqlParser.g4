parser grammar HqlParser;
import HqlParserBase;

options {
	tokenVocab=HqlLexer;
}

function
	: standardFunction
	| aggregateFunction
	| jpaCollectionFunction
	| hqlCollectionFunction
	| jpaNonStandardFunction
	| genericFunction
	;

aggregateFunction
	: everyFunction
	| anyFunction
	;

genericFunction
	: genericFunctionName LEFT_PAREN (nonStandardFunctionArguments | ASTERISK)? RIGHT_PAREN
	;

genericFunctionName
	: dotIdentifierSequence
	;

nonStandardFunctionArguments
	: (DISTINCT | datetimeField COMMA)? expression (COMMA expression)*
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
	;

