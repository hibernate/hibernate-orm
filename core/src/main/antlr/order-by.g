header
{
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.sql.ordering.antlr;
}
/**
 * Antlr grammar for dealing with <tt>order-by</tt> mapping fragments.

 * @author Steve Ebersole
 */
class GeneratedOrderByFragmentParser extends Parser;

options
{
	exportVocab=OrderByTemplate;
	buildAST=true;
	k=3;
}

tokens
{
    // synthetic tokens
    ORDER_BY;
    SORT_SPEC;
    ORDER_SPEC;
    SORT_KEY;
    EXPR_LIST;
    DOT;
    IDENT_LIST;
    COLUMN_REF;

    COLLATE="collate";
	ASCENDING="asc";
	DESCENDING="desc";
}


{
    /**
     * Method for logging execution trace information.
     *
     * @param msg The trace message.
     */
    protected void trace(String msg) {
        System.out.println( msg );
    }

    /**
     * Extract a node's text.
     *
     * @param ast The node
     *
     * @return The text.
     */
    protected final String extractText(AST ast) {
        // for some reason, within AST creation blocks "[]" I am somtimes unable to refer to the AST.getText() method
        // using #var (the #var is not interpreted as the rule's output AST).
        return ast.getText();
    }

    /**
     * Process the given node as a quote identifier.  These need to be quoted in the dialect-specific way.
     *
     * @param ident The quoted-identifier node.
     *
     * @return The processed node.
     *
     * @see org.hibernate.dialect.Dialect#quote
     */
    protected AST quotedIdentifier(AST ident) {
        return ident;
    }

    /**
     * Process the given node as a quote string.
     *
     * @param ident The quoted string.  This is used from within function param recognition, and represents a
     * SQL-quoted string.
     *
     * @return The processed node.
     */
    protected AST quotedString(AST ident) {
        return ident;
    }

    /**
     * A check to see if the text of the given node represents a known function name.
     *
     * @param ast The node whose text we want to check.
     *
     * @return True if the node's text is a known function name, false otherwise.
     *
     * @see org.hibernate.dialect.function.SQLFunctionRegistry
     */
    protected boolean isFunctionName(AST ast) {
        return false;
    }

    /**
     * Process the given node as a function.
     *
     * @param The node representing the function invocation (including parameters as subtree components).
     *
     * @return The processed node.
     */
    protected AST resolveFunction(AST ast) {
        return ast;
    }

    /**
     * Process the given node as an IDENT.  May represent either a column reference or a property reference.
     *
     * @param ident The node whose text represents either a column or property reference.
     *
     * @return The processed node.
     */
    protected AST resolveIdent(AST ident) {
        return ident;
    }

    /**
     * Allow post processing of each <tt>sort specification</tt>
     *
     * @param The grammar-built sort specification subtree.
     *
     * @return The processed sort specification subtree.
     */
    protected AST postProcessSortSpecification(AST sortSpec) {
        return sortSpec;
    }

}

/**
 * Main recognition rule for this grammar
 */
orderByFragment { trace("orderByFragment"); }
    : sortSpecification ( COMMA! sortSpecification  )* {
        #orderByFragment = #( [ORDER_BY, "order-by"], #orderByFragment );
    }
    ;

/**
 * Reconition rule for what ANSI SQL terms the <tt>sort specification</tt>, which is essentially each thing upon which
 * the results should be sorted.
 */
sortSpecification { trace("sortSpecification"); }
    : sortKey (collationSpecification)? (orderingSpecification)? {
        #sortSpecification = #( [SORT_SPEC, "{sort specification}"], #sortSpecification );
        #sortSpecification = postProcessSortSpecification( #sortSpecification );
    }
    ;

/**
 * Reconition rule for what ANSI SQL terms the <tt>sort key</tt> which is the expression (column, function, etc) upon
 * which to base the sorting.
 */
sortKey! { trace("sortKey"); }
    : e:expression {
        #sortKey = #( [SORT_KEY, "sort key"], #e );
    }
    ;

/**
 * Reconition rule what this grammar recognizes as valid <tt>sort key</tt>.
 */
expression! { trace("expression"); }
    : HARD_QUOTE qi:IDENT HARD_QUOTE {
        #expression = quotedIdentifier( #qi );
    }
    | ( IDENT (DOT IDENT)* OPEN_PAREN ) => f:functionCall {
        #expression = #f;
    }
    | p:simplePropertyPath {
        #expression = resolveIdent( #p );
    }
    | i:IDENT {
        if ( isFunctionName( #i ) ) {
            #expression = resolveFunction( #i );
        }
        else {
            #expression = resolveIdent( #i );
        }
    }
    ;

/**
 * Intended for use as a syntactic predicate to determine whether an IDENT represents a known SQL function name.
 */
functionCallCheck! { trace("functionCallCheck"); }
    : IDENT (DOT IDENT)* OPEN_PAREN { true }?
    ;

/**
 * Recognition rule for a function call
 */
functionCall! { trace("functionCall"); }
    : fn:functionName OPEN_PAREN pl:functionParameterList CLOSE_PAREN {
        #functionCall = #( [IDENT, extractText( #fn )], #pl );
        #functionCall = resolveFunction( #functionCall );
    }
    ;

/**
 * A function-name is an IDENT followed by zero or more (DOT IDENT) sequences
 */
functionName {
        trace("functionName");
        StringBuffer buffer = new StringBuffer();
    }
    : i:IDENT { buffer.append( i.getText() ); }
            ( DOT i2:IDENT { buffer.append( '.').append( i2.getText() ); } )* {
        #functionName = #( [IDENT,buffer.toString()] );
    }
    ;

/**
 * Recognition rule used to "wrap" all function parameters into an EXPR_LIST node
 */
functionParameterList { trace("functionParameterList"); }
    : functionParameter ( COMMA! functionParameter )* {
        #functionParameterList = #( [EXPR_LIST, "{param list}"], #functionParameterList );
    }
    ;

/**
 * Recognized function parameters.
 */
functionParameter { trace("functionParameter"); }
    : expression
    | NUM_DOUBLE
    | NUM_FLOAT
    | NUM_INT
    | NUM_LONG
    | QUOTED_STRING {
        #functionParameter = quotedString( #functionParameter );
    }
    ;

/**
 * Reconition rule for what ANSI SQL terms the <tt>collation specification</tt> used to allow specifying that sorting for
 * the given {@link #sortSpecification} be treated within a specific character-set.
 */
collationSpecification! { trace("collationSpecification"); }
    : c:COLLATE cn:collationName {
        #collationSpecification = #( [COLLATE, extractText( #cn )] );
    }
    ;

/**
 * The collation name wrt {@link #collationSpecification}.  Namely, the character-set.
 */
collationName { trace("collationSpecification"); }
    : IDENT
    ;

/**
 * Reconition rule for what ANSI SQL terms the <tt>ordering specification</tt>; <tt>ASCENDING</tt> or
 * <tt>DESCENDING</tt>.
 */
orderingSpecification! { trace("orderingSpecification"); }
    : ( "asc" | "ascending" ) {
        #orderingSpecification = #( [ORDER_SPEC, "asc"] );
    }
    | ( "desc" | "descending") {
        #orderingSpecification = #( [ORDER_SPEC, "desc"] );
    }
    ;

/**
 * A simple-property-path is an IDENT followed by one or more (DOT IDENT) sequences
 */
simplePropertyPath {
        trace("simplePropertyPath");
        StringBuffer buffer = new StringBuffer();
    }
    : i:IDENT { buffer.append( i.getText() ); }
            ( DOT i2:IDENT { buffer.append( '.').append( i2.getText() ); } )+ {
        #simplePropertyPath = #( [IDENT,buffer.toString()] );
    }
    ;


// **** LEXER ******************************************************************

/**
 * Lexer for the <tt>order-by</tt> fragment parser

 * @author Steve Ebersole
 * @author Joshua Davis
 */
class GeneratedOrderByLexer extends Lexer;

options {
	exportVocab=OrderByTemplate;
	testLiterals = false;
	k=2;
	charVocabulary='\u0000'..'\uFFFE';	// Allow any char but \uFFFF (16 bit -1, ANTLR's EOF character)
	caseSensitive = false;
	caseSensitiveLiterals = false;
}

// -- Keywords --

OPEN_PAREN: '(';
CLOSE_PAREN: ')';

COMMA: ',';

HARD_QUOTE: '`';

IDENT options { testLiterals=true; }
	: ID_START_LETTER ( ID_LETTER )*
	;

protected
ID_START_LETTER
    :    '_'
    |    '$'
    |    'a'..'z'
    |    '\u0080'..'\ufffe'       // HHH-558 : Allow unicode chars in identifiers
    ;

protected
ID_LETTER
    :    ID_START_LETTER
    |    '0'..'9'
    ;

QUOTED_STRING
	  : '\'' ( (ESCqs)=> ESCqs | ~'\'' )* '\''
	;

protected
ESCqs
	:
		'\'' '\''
	;

//--- From the Java example grammar ---
// a numeric literal
NUM_INT
	{boolean isDecimal=false; Token t=null;}
	:   '.' {_ttype = DOT;}
			(	('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
				{
					if (t != null && t.getText().toUpperCase().indexOf('F')>=0)
					{
						_ttype = NUM_FLOAT;
					}
					else
					{
						_ttype = NUM_DOUBLE; // assume double
					}
				}
			)?
	|	(	'0' {isDecimal = true;} // special case for just '0'
			(	('x')
				(											// hex
					// the 'e'|'E' and float suffix stuff look
					// like hex digits, hence the (...)+ doesn't
					// know when to stop: ambig.  ANTLR resolves
					// it correctly by matching immediately.  It
					// is therefore ok to hush warning.
					options { warnWhenFollowAmbig=false; }
				:	HEX_DIGIT
				)+
			|	('0'..'7')+									// octal
			)?
		|	('1'..'9') ('0'..'9')*  {isDecimal=true;}		// non-zero decimal
		)
		(	('l') { _ttype = NUM_LONG; }

		// only check to see if it's a float if looks like decimal so far
		|	{isDecimal}?
			(   '.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
			|   EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
			|   f4:FLOAT_SUFFIX {t=f4;}
			)
			{
				if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0)
				{
					_ttype = NUM_FLOAT;
				}
				else
				{
					_ttype = NUM_DOUBLE; // assume double
				}
			}
		)?
	;

// hexadecimal digit (again, note it's protected!)
protected
HEX_DIGIT
	:	('0'..'9'|'a'..'f')
	;

// a couple protected methods to assist in matching floating point numbers
protected
EXPONENT
	:	('e') ('+'|'-')? ('0'..'9')+
	;

protected
FLOAT_SUFFIX
	:	'f'|'d'
	;

WS  :   (   ' '
		|   '\t'
		|   '\r' '\n' { newline(); }
		|   '\n'      { newline(); }
		|   '\r'      { newline(); }
		)
		{$setType(Token.SKIP);} //ignore this token
	;
