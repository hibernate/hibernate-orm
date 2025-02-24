parser grammar OrderingParser;

options {
	tokenVocab=OrderingLexer;
}

@header {
/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.grammars.ordering;

/**
 * Grammar for parsing order-by fragments.
 *
 * @implNote While we could re-use the HQL lexer/parser for order fragment parsing, both the HQL lexer and parser
 * are way "heavier" than needed here.  So we use a simplified lexer and parser that defione just what is needed
 * to parse the order fragment
 */
}

// todo (6.0) : add hooks for keyword-as-identifier logging like we do for HQL?

orderByFragment
	: sortSpecification (COMMA sortSpecification)*
	;

sortSpecification
	: expression collationSpecification? direction? nullsPrecedence?
	;

expression
	: function		# FunctionExpression
	| identifier	# IdentifierExpression
	| dotIdentifier	# DotIdentifierExpression
	;

function
	: simpleFunction
	| packagedFunction
	;

simpleFunction
	: identifier functionArguments
	;

packagedFunction
	: dotIdentifier functionArguments
	;

functionArguments
	: OPEN_PAREN (functionArgument ( COMMA functionArgument )* )? CLOSE_PAREN
	;

functionArgument
	: expression
    | literal
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
	;

collationSpecification
	: COLLATE identifier
	;

direction
	: ASC | DESC
	;

nullsPrecedence
	: NULLS (FIRST | LAST)
	;

identifier
	: IDENTIFIER
	| QUOTED_IDENTIFIER
	// keyword-as-identifier
	| FIRST
	| LAST
	| ASC
	| DESC
	| COLLATE
	;

dotIdentifier
	: identifier (DOT identifier)+
	;
