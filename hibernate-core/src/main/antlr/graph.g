header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.graph.internal.parse;
}

/**
 * Antlr grammar describing the Hibernate EntityGraph Language.
 */
class GeneratedGraphParser extends Parser;

options {
    // call the vocabulary (H)ibernate (E)ntity(G)raph (L)anguage
	exportVocab=HEGL;

    k = 2;

//	buildAST = true;
	buildAST = false;
}


{
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// semantic actions/callouts

	protected void startAttribute(Token attributeName) {
	}

	protected void startQualifiedAttribute(Token attributeName, Token qualifier) {
	}

	protected void finishAttribute() {
	}

	protected void startSubGraph(Token subType) {
	}

	protected void finishSubGraph() {
	}
}


graph
 	: attributeNode (COMMA attributeNode)*
 	;

attributeNode
	: attributePath (subGraph)? { finishAttribute(); }
	;

attributePath
	: path:NAME (DOT qualifier:NAME)? {
		if ( qualifier == null ) {
			startAttribute( path );
		}
		else {
			startQualifiedAttribute( path, qualifier );
		}
	}
	;

subGraph
	: LPAREN (subtype:NAME COLON)? { startSubGraph( subtype ); } attributeNode (COMMA attributeNode )* RPAREN {
		finishSubGraph();
	}
	;



// **** LEXER ******************************************************************

/**
 * Lexer for the Hibernate EntityGraph Language grammar
 */
class GraphLexer extends Lexer;

options {
    // call the vocabulary (H)ibernate (E)ntity(G)raph (L)anguage
	exportVocab=HEGL;

    k = 2;

    // Allow any char but \uffff (16 bit -1, ANTLR's EOF character)
    charVocabulary = '\u0000'..'\ufffe';

	caseSensitive = false;
	testLiterals = false;
}

COLON: ':';

COMMA: ',';

DOT: '.';

LPAREN: '(';

RPAREN: ')';

WHITESPACE
	:   (   ' '
		|   '\t'
		|   '\r' '\n' { newline(); }
		|   '\n'      { newline(); }
		|   '\r'      { newline(); }
		)
		{$setType(Token.SKIP);} //ignore this token
	;


/**
 * In this grammar, basically any string since we (atm) have no keywords
 */
NAME
	: NAME_START ( NAME_CONTINUATION )*
	;

protected
NAME_START
    :    '_'
    |    '$'
    |    'a'..'z'
    // HHH-558 : Allow unicode chars in identifiers
    //|    '\u0080'..'\ufffe'
    ;

protected
NAME_CONTINUATION
    :    NAME_START
    |    '0'..'9'
    ;
