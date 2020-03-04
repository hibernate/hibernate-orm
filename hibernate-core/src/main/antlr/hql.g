header
{
package org.hibernate.hql.internal.antlr;

import org.hibernate.hql.internal.ast.*;
import org.hibernate.hql.internal.ast.util.*;

}
/**
 * Hibernate Query Language Grammar
 * <br>
 * This grammar parses the query language for Hibernate (an Open Source, Object-Relational
 * mapping library).  A partial BNF grammar description is available for reference here:
 * http://www.hibernate.org/Documentation/HQLBNF
 *
 * Text from the original reference BNF is prefixed with '//##'.
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
class HqlBaseParser extends Parser;

options
{
	exportVocab=Hql;
	buildAST=true;
	k=3;    // For 'not like', 'not in', etc.
}

tokens
{
	// -- HQL Keyword tokens --
	ALL="all";
	ANY="any";
	AND="and";
	AS="as";
	ASCENDING="asc";
	AVG="avg";
	BETWEEN="between";
	CLASS="class";
	COUNT="count";
	DELETE="delete";
	DESCENDING="desc";
	DOT;
	DISTINCT="distinct";
	ELEMENTS="elements";
	ESCAPE="escape";
	EXISTS="exists";
	FALSE="false";
	FETCH="fetch";
	FROM="from";
	FULL="full";
	GROUP="group";
	HAVING="having";
	IN="in";
	INDICES="indices";
	INNER="inner";
	INSERT="insert";
	INTO="into";
	IS="is";
	JOIN="join";
	LEFT="left";
	LIKE="like";
	MAX="max";
	MIN="min";
	NEW="new";
	NOT="not";
	NULL="null";
	OR="or";
	ORDER="order";
	OUTER="outer";
	PROPERTIES="properties";
	RIGHT="right";
	SELECT="select";
	SET="set";
	SOME="some";
	SUM="sum";
	TRUE="true";
	UPDATE="update";
	VERSIONED="versioned";
	WHERE="where";
	NULLS="nulls";
	FIRST;
	LAST;

	// -- SQL tokens --
	// These aren't part of HQL, but the SQL fragment parser uses the HQL lexer, so they need to be declared here.
	CASE="case";	// a "searched case statement", whereas CASE2 represents a "simple case statement"
	END="end";
	ELSE="else";
	THEN="then";
	WHEN="when";
	ON="on";
	WITH="with";

	// -- JPAQL tokens --
	BOTH="both";
	EMPTY="empty";
	LEADING="leading";
	MEMBER="member";
	OBJECT="object";
	OF="of";
	TRAILING="trailing";
	KEY;
	VALUE;
	ENTRY;

	// -- Synthetic token types --
	AGGREGATE;		// One of the aggregate functions (e.g. min, max, avg)
	ALIAS;
	CONSTRUCTOR;
	CASE2;			// a "simple case statement", whereas CASE represents a "searched case statement"
	CAST;
	COLL_PATH;
	EXPR_LIST;
	FILTER_ENTITY;		// FROM element injected because of a filter expression (happens during compilation phase 2)
	IN_LIST;
	INDEX_OP;
	IS_NOT_NULL;
	IS_NULL;			// Unary 'is null' operator.
	METHOD_CALL;
	NOT_BETWEEN;
	NOT_IN;
	NOT_LIKE;
	ORDER_ELEMENT;
	QUERY;
	RANGE;
	ROW_STAR;
	SELECT_FROM;
	COLL_SIZE;
	UNARY_MINUS;
	UNARY_PLUS;
	VECTOR_EXPR;		// ( x, y, z )
	WEIRD_IDENT;		// Identifiers that were keywords when they came in.

	// Literal tokens.
	CONSTANT;
	NUM_DOUBLE;
	NUM_FLOAT;
	NUM_LONG;
	NUM_BIG_INTEGER;
	NUM_BIG_DECIMAL;
	JAVA_CONSTANT;
}

{
    /** True if this is a filter query (allow no FROM clause). **/
	private boolean filter = false;

	/**
	 * Sets the filter flag.
	 * @param f True for a filter query, false for a normal query.
	 */
	public void setFilter(boolean f) {
		filter = f;
	}

	/**
	 * Returns true if this is a filter query, false if not.
	 * @return true if this is a filter query, false if not.
	 */
	public boolean isFilter() {
		return filter;
	}

	/**
	 * This method is overriden in the sub class in order to provide the
	 * 'keyword as identifier' hack.
	 * @param token The token to retry as an identifier.
	 * @param ex The exception to throw if it cannot be retried as an identifier.
	 */
	public AST handleIdentifierError(Token token,RecognitionException ex) throws RecognitionException, TokenStreamException {
		// Base implementation: Just re-throw the exception.
		throw ex;
	}

    /**
     * This method looks ahead and converts . <token> into . IDENT when
     * appropriate.
     */
    public void handleDotIdent() throws TokenStreamException {
    }

	/**
	 * Returns the negated equivalent of the expression.
	 * @param x The expression to negate.
	 */
	public AST negateNode(AST x) {
		// Just create a 'not' parent for the default behavior.
		return ASTUtil.createParent(astFactory, NOT, "not", x);
	}

	/**
	 * Returns the 'cleaned up' version of a comparison operator sub-tree.
	 * @param x The comparison operator to clean up.
	 */
	public AST processEqualityExpression(AST x) throws RecognitionException {
		return x;
	}

	public void weakKeywords() throws TokenStreamException {
	}

	public void firstPathTokenWeakKeywords() throws TokenStreamException {
	}

	public void handlePrimaryExpressionDotIdent() throws TokenStreamException {
	}

    /**
     * Manages the case of an optional FROM allowing the path to start with the "from" keyword
     */
	public void matchOptionalFrom() throws RecognitionException, TokenStreamException {
	}

	/**
	 * Called after we have recognized ':'.  The expectation is to handle converting
	 * any non-IDENT token where possibleID == true into an IDENT
	 */
	public void expectNamedParameterName() throws TokenStreamException {
	}

	public void processMemberOf(Token n,AST p,ASTPair currentAST) {
	}

	protected boolean validateSoftKeyword(String text) throws TokenStreamException {
		return validateLookAheadText(1, text);
	}

	protected boolean validateLookAheadText(int lookAheadPosition, String text) throws TokenStreamException {
		String text2Validate = retrieveLookAheadText( lookAheadPosition );
		return text2Validate == null ? false : text2Validate.equalsIgnoreCase( text );
	}

	protected String retrieveLookAheadText(int lookAheadPosition) throws TokenStreamException {
		Token token = LT(lookAheadPosition);
		return token == null ? null : token.getText();
	}

    protected String unquote(String text) {
        return text.substring( 1, text.length() - 1 );
    }

    protected void registerTreat(AST pathToTreat, AST treatAs) {
    }
}

statement
	: ( updateStatement | deleteStatement | selectStatement | insertStatement ) (EOF!)
	;

// Without the optionalVersioned if the path starts with a keyword the parser fails
updateStatement
	: UPDATE^ optionalVersioned
		optionalFromTokenFromClause
		setClause
		(whereClause)?
	;

optionalVersioned
    : (VERSIONED)?
    exception
    catch [NoViableAltException ex]
 	{
 	    // ignore
 	}
    ;

setClause
	: (SET^ assignment (COMMA! assignment)*)
	;

assignment
	: stateField EQ^ newValue
	;

// "state_field" is the term used in the EJB3 sample grammar; used here for easy reference.
// it is basically a property ref
stateField
	: path
	;

// this still needs to be defined in the ejb3 spec; additiveExpression is currently just a best guess,
// although it is highly likely I would think that the spec may limit this even more tightly.
newValue
	: concatenation
	;

deleteStatement
	: DELETE^
		(optionalFromTokenFromClause)
		(whereClause)?
	;

optionalFromTokenFromClause!
	: {matchOptionalFrom();} f:path (a:asAlias)? {
		AST #range = #([RANGE, "RANGE"], #f, #a);
		#optionalFromTokenFromClause = #([FROM, "FROM"], #range);
	}
	;

selectStatement
	: queryRule {
		#selectStatement = #([QUERY,"query"], #selectStatement);
	}
	;

insertStatement
	// Would be nice if we could abstract the FromClause/FromElement logic
	// out such that it could be reused here; something analogous to
	// a "table" rule in sql-grammars
	: INSERT^ intoClause selectStatement
	;

intoClause
	: INTO^ path { weakKeywords(); } insertablePropertySpec
	;

insertablePropertySpec
	: OPEN! primaryExpression ( COMMA! primaryExpression )* CLOSE! {
		// Just need *something* to distinguish this on the hql-sql.g side
		#insertablePropertySpec = #([RANGE, "column-spec"], #insertablePropertySpec);
	}
	;

//## query:
//##     [selectClause] fromClause [whereClause] [groupByClause] [havingClause] [orderByClause];

queryRule
	: selectFrom
		(whereClause)?
		(groupByClause)?
		(orderByClause)?
		;

selectFrom!
	:  (s:selectClause)? (f:fromClause)? {
		// If there was no FROM clause and this is a filter query, create a from clause.  Otherwise, throw
		// an exception because non-filter queries must have a FROM clause.
		if (#f == null) {
			if (filter) {
				#f = #([FROM,"{filter-implied FROM}"]);
			}
			else
				throw new SemanticException("FROM expected (non-filter queries must contain a FROM clause)");
		}
			
		// Create an artificial token so the 'FROM' can be placed
		// before the SELECT in the tree to make tree processing
		// simpler.
		#selectFrom = #([SELECT_FROM,"SELECT_FROM"],f,s);
	}
	;

//## selectClause:
//##     SELECT DISTINCT? selectedPropertiesList | ( NEW className OPEN selectedPropertiesList CLOSE );

selectClause
	: SELECT^	// NOTE: The '^' after a token causes the corresponding AST node to be the root of the sub-tree.
		{ weakKeywords(); }	// Weak keywords can appear immediately after a SELECT token.
		(DISTINCT)? ( selectedPropertiesList | newExpression | selectObject )
	;

newExpression
	: (NEW! path) op:OPEN^ {#op.setType(CONSTRUCTOR);} selectedPropertiesList CLOSE!
	;

selectObject
   : OBJECT^ OPEN! identifier CLOSE!
   ;

//## fromClause:
//##    FROM className AS? identifier (  ( COMMA className AS? identifier ) | ( joinType path AS? identifier ) )*;

// NOTE: This *must* begin with the "FROM" token, otherwise the sub-query rule will be ambiguous
// with the expression rule.
// Also note: after a comma weak keywords are allowed and should be treated as identifiers.

fromClause
	: FROM^ { weakKeywords(); } fromRange ( fromJoin | COMMA! { weakKeywords(); } fromRange )*
	;

//## joinType:
//##     ( ( 'left'|'right' 'outer'? ) | 'full' | 'inner' )? JOIN FETCH?;

fromJoin
	: ( ( ( LEFT | RIGHT ) (OUTER)? ) | FULL | INNER )? JOIN^ (FETCH)? 
	        joinPath (asAlias)? (propertyFetch)? (withClause)?
	;

joinPath
    : { validateSoftKeyword("treat") && LA(2) == OPEN }? castedJoinPath
    | path
    ;

/**
 * Represents the JPA 2.1 TREAT construct when applied to a join.  Hibernate already handles subclass
 * property references implicitly, so we simply "eat" all tokens of the TREAT construct and just return the
 * join path itself.
 *
 * Uses a validating semantic predicate to make sure the text of the matched first IDENT is the TREAT keyword
 */
castedJoinPath
    : i:IDENT! OPEN! p:path AS! a:path! CLOSE! {i.getText().equalsIgnoreCase("treat") }? {
        registerTreat( #p, #a );
    }
    ;

withClause
	: WITH^ logicalExpression
	// JPA 2.1 support for an ON clause that isn't really an ON clause...
	| ON! le:logicalExpression {
	    // it's really just a WITH clause, so treat it as such...
	    #withClause = #( [WITH, "with"], #le );
	}
	;

fromRange
	: fromClassOrOuterQueryPath
	| inClassDeclaration
	| inCollectionDeclaration
	| inCollectionElementsDeclaration
	;
	
fromClassOrOuterQueryPath!
	: c:path { weakKeywords(); } (a:asAlias)? (p:propertyFetch)? {
		#fromClassOrOuterQueryPath = #([RANGE, "RANGE"], #c, #a, #p);
	}
	;

inClassDeclaration!
	: a:alias IN! CLASS! c:path {
		#inClassDeclaration = #([RANGE, "RANGE"], #c, #a);
	}
	;

inCollectionDeclaration!
    : IN! OPEN! p:path CLOSE! a:asAlias {
        #inCollectionDeclaration = #([JOIN, "join"], [INNER, "inner"], #p, #a);
	}
    ;

inCollectionElementsDeclaration!
	: a:alias IN! ELEMENTS! OPEN! p:path CLOSE! {
        #inCollectionElementsDeclaration = #([JOIN, "join"], [INNER, "inner"], #p, #a);
	}
    ;

// Alias rule - Parses the optional 'as' token and forces an AST identifier node.
asAlias
	: (AS!)? alias
	;

alias
	: a:identifier { #a.setType(ALIAS); }
    ;
    
propertyFetch
	: FETCH ALL! PROPERTIES!
	;

//## groupByClause:
//##     GROUP_BY path ( COMMA path )*;

groupByClause
	: GROUP^
		"by"! expression ( COMMA! expression )*
		(havingClause)?
	;

//## orderByClause:
//##     ORDER_BY selectedPropertiesList;

orderByClause
	: ORDER^ "by"! orderElement ( COMMA! orderElement )*
	;

orderElement
	: expression ( ascendingOrDescending )? ( nullOrdering )?
	;

ascendingOrDescending
	: ( "asc" | "ascending" )	{ #ascendingOrDescending.setType(ASCENDING); }
	| ( "desc" | "descending") 	{ #ascendingOrDescending.setType(DESCENDING); }
	;

nullOrdering
    : NULLS nullPrecedence
    ;

nullPrecedence
    : IDENT {
            if ( "first".equalsIgnoreCase( #nullPrecedence.getText() ) ) {
                #nullPrecedence.setType( FIRST );
            }
            else if ( "last".equalsIgnoreCase( #nullPrecedence.getText() ) ) {
                #nullPrecedence.setType( LAST );
            }
            else {
                throw new SemanticException( "Expecting 'first' or 'last', but found '" +  #nullPrecedence.getText() + "' as null ordering precedence." );
            }
    }
    ;

//## havingClause:
//##     HAVING logicalExpression;

havingClause
	: HAVING^ logicalExpression
	;

//## whereClause:
//##     WHERE logicalExpression;

whereClause
	: WHERE^ logicalExpression
	;

//## selectedPropertiesList:
//##     ( path | aggregate ) ( COMMA path | aggregate )*;

selectedPropertiesList
	: aliasedExpression ( COMMA! aliasedExpression )*
	;
	
aliasedExpression
	: expression ( AS^ identifier )?
	;

// expressions
// Note that most of these expressions follow the pattern
//   thisLevelExpression :
//       nextHigherPrecedenceExpression
//           (OPERATOR nextHigherPrecedenceExpression)*
// which is a standard recursive definition for a parsing an expression.
//
// Operator precedence in HQL
// lowest  --> ( 7)  OR
//             ( 6)  AND, NOT
//             ( 5)  equality: ==, <>, !=, is
//             ( 4)  relational: <, <=, >, >=,
//                   LIKE, NOT LIKE, BETWEEN, NOT BETWEEN, IN, NOT IN
//             ( 3)  addition and subtraction: +(binary) -(binary)
//             ( 2)  multiplication: * / %, concatenate: ||
// highest --> ( 1)  +(unary) -(unary)
//                   []   () (method call)  . (dot -- identifier qualification)
//                   aggregate function
//                   ()  (explicit parenthesis)
//
// Note that the above precedence levels map to the rules below...
// Once you have a precedence chart, writing the appropriate rules as below
// is usually very straightfoward

logicalExpression
	: expression
	;

// Main expression rule
expression
	: logicalOrExpression
	;

// level 7 - OR
logicalOrExpression
	: logicalAndExpression ( OR^ logicalAndExpression )*
	;

// level 6 - AND, NOT
logicalAndExpression
	: negatedExpression ( AND^ negatedExpression )*
	;

// NOT nodes aren't generated.  Instead, the operator in the sub-tree will be
// negated, if possible.   Expressions without a NOT parent are passed through.
negatedExpression!
{ weakKeywords(); } // Weak keywords can appear in an expression, so look ahead.
	: NOT^ x:negatedExpression { #negatedExpression = negateNode(#x); }
	| y:equalityExpression { #negatedExpression = #y; }
	;

//## OP: EQ | LT | GT | LE | GE | NE | SQL_NE | LIKE;

// level 5 - EQ, NE
equalityExpression
	: x:relationalExpression (
		( EQ^
		| is:IS^	{ #is.setType(EQ); } (NOT! { #is.setType(NE); } )?
		| NE^
		| ne:SQL_NE^	{ #ne.setType(NE); }
		) y:relationalExpression)* {
			// Post process the equality expression to clean up 'is null', etc.
			#equalityExpression = processEqualityExpression(#equalityExpression);
		}
	;

// level 4 - LT, GT, LE, GE, LIKE, NOT LIKE, BETWEEN, NOT BETWEEN
// NOTE: The NOT prefix for LIKE and BETWEEN will be represented in the
// token type.  When traversing the AST, use the token type, and not the
// token text to interpret the semantics of these nodes.
relationalExpression
	: concatenation (
		( ( ( LT^ | GT^ | LE^ | GE^ ) additiveExpression )* )
		// Disable node production for the optional 'not'.
		| (n:NOT!)? (
			// Represent the optional NOT prefix using the token type by
			// testing 'n' and setting the token type accordingly.
			(i:IN^ {
					#i.setType( (n == null) ? IN : NOT_IN);
					#i.setText( (n == null) ? "in" : "not in");
				}
				inList)
			| (b:BETWEEN^ {
					#b.setType( (n == null) ? BETWEEN : NOT_BETWEEN);
					#b.setText( (n == null) ? "between" : "not between");
				}
				betweenList )
			| (l:LIKE^ {
					#l.setType( (n == null) ? LIKE : NOT_LIKE);
					#l.setText( (n == null) ? "like" : "not like");
				}
				concatenation likeEscape)
			| (MEMBER! (OF!)? p:memberOfPath! {
				processMemberOf(n,#p,currentAST);
			  } ) )
		)
	;

likeEscape
	: (ESCAPE^ concatenation)?
	;

inList
	: x:compoundExpr
	{ #inList = #([IN_LIST,"inList"], #inList); }
	;

betweenList
	: concatenation AND! concatenation
	;

memberOfPath
	// JPA says this is a `collection_valued_path_expression` which is essentially either:
	// 		1) a treated path followed by a collection-valued attribute reference
	//		2) a path
	: { validateSoftKeyword("treat") && LA(2) == OPEN }? i:IDENT! OPEN! p:path AS! a:path! CLOSE! ( DOT^ path )
	| path
	;

//level 4 - string concatenation
concatenation
	: additiveExpression 
	( c:CONCAT^ { #c.setType(EXPR_LIST); #c.setText("concatList"); } 
	  additiveExpression
	  ( CONCAT! additiveExpression )* 
	  { #concatenation = #([METHOD_CALL, "||"], #([IDENT, "concat"]), #c ); } )?
	;

// level 3 - binary plus and minus
additiveExpression
	: multiplyExpression ( ( PLUS^ | MINUS^ ) multiplyExpression )*
	;

// level 2 - binary multiply and divide
multiplyExpression
	: unaryExpression ( ( STAR^ | DIV^ | MOD^ ) unaryExpression )*
	;
	
// level 1 - unary minus, unary plus, not
unaryExpression
	: MINUS^ {#MINUS.setType(UNARY_MINUS);} unaryExpression
	| PLUS^ {#PLUS.setType(UNARY_PLUS);} unaryExpression
	| caseExpression
	| quantifiedExpression
	| atom
	;

caseExpression
	// NOTE : the unaryExpression rule contains the subQuery rule
	: simpleCaseStatement
	| searchedCaseStatement
	;

simpleCaseStatement
	: CASE^ unaryExpression (simpleCaseWhenClause)+ (elseClause)? END! {
		#simpleCaseStatement.setType(CASE2);
	}
	;

simpleCaseWhenClause
	: (WHEN^ unaryExpression THEN! unaryExpression)
	;

elseClause
	: (ELSE^ unaryExpression)
	;

searchedCaseStatement
	: CASE^ (searchedCaseWhenClause)+ (elseClause)? END!
	;

searchedCaseWhenClause
	: (WHEN^ logicalExpression THEN! unaryExpression)
	;
	
quantifiedExpression
	: ( SOME^ | EXISTS^ | ALL^ | ANY^ ) 
	( identifier | collectionExpr | (OPEN! ( subQuery ) CLOSE!) )
	;

// level 0 - expression atom
//      * ident qualifier ('.' ident )
//      * array index ( [ expr ] )
//      * method call ( '.' ident '(' exprList ') )
//      * function : differentiated from method call via explicit keyword
atom
	: {handlePrimaryExpressionDotIdent();} primaryExpression
		(
			DOT^ identifier
				( options { greedy=true; } :
					( op:OPEN^ {#op.setType(METHOD_CALL);} exprList CLOSE! ) )?
		|	lb:OPEN_BRACKET^ {#lb.setType(INDEX_OP);} expression CLOSE_BRACKET!
		)*
	;


// level 0 - the basic element of an expression
primaryExpression
    : { validateSoftKeyword("function") && LA(2) == OPEN && LA(3) == QUOTED_STRING }? jpaFunctionSyntax
    | { validateSoftKeyword("cast") && LA(2) == OPEN }? castFunction
    | { validateSoftKeyword("size") && LA(2) == OPEN }? collectionSizeFunction
	| identPrimary ( options {greedy=true;} : DOT^ "class" )?
	| constant
	| parameter
	| OPEN! (expressionOrVector | subQuery) CLOSE!
	;

jpaFunctionSyntax!
    : i:IDENT OPEN n:QUOTED_STRING (COMMA a:exprList)? CLOSE {
    	final String functionName = unquote( #n.getText() );

    	if ( functionName.equalsIgnoreCase( "cast" ) ) {
			#i.setType( CAST );
			#i.setText( #i.getText() + " (" + functionName + ")" );
			final AST expression = #a.getFirstChild();
			final AST type = expression.getNextSibling();
    		#jpaFunctionSyntax = #( #i, expression, type );
    	}
    	else {
			#i.setType( METHOD_CALL );
			#i.setText( #i.getText() + " (" + functionName + ")" );
			#jpaFunctionSyntax = #( #i, [IDENT, unquote( #n.getText() )], #a );
    	}
    }
    ;

castFunction!
	: c:IDENT OPEN e:expression (AS)? t:castTargetType CLOSE {
		#c.setType( CAST );
		#castFunction = #( #c, #e, #t );
	}
	;

castTargetType
	// the cast target type is Hibernate type name which is either:
	//		1) a simple identifier
	//		2) a simple identifier-(dot-identifier)* sequence
	: identifier { handleDotIdent(); } ( options { greedy=true; } : DOT^ identifier )*
	;

collectionSizeFunction!
	: s:IDENT OPEN p:collectionPath CLOSE {
		assert #s.getText().equalsIgnoreCase( "size" );
		#collectionSizeFunction = #( [COLL_SIZE], #p );
	}
	;

collectionPath!
// for now we do not support nested path refs (for embeddables)
	: simpleRef:identifier {
		#collectionPath = #( [COLL_PATH], #simpleRef );
	}
	| qualifier:collectionPathQualifier DOT propertyName:identifier {
		#collectionPath = #( [COLL_PATH], #propertyName, #qualifier );
	};

collectionPathQualifier
	: identifier ( DOT^ { weakKeywords(); } identifier )*
	;

parameter
	: COLON^ { expectNamedParameterName(); } IDENT
	| PARAM^ (NUM_INT)?
	;

// This parses normal expression and a list of expressions separated by commas.  If a comma is encountered
// a parent VECTOR_EXPR node will be created for the list.
expressionOrVector!
	: e:expression ( v:vectorExpr )? {
		// If this is a vector expression, create a parent node for it.
		if (#v != null)
			#expressionOrVector = #([VECTOR_EXPR,"{vector}"], #e, #v);
		else
			#expressionOrVector = #e;
	}
	;

vectorExpr
	: COMMA! expression (COMMA! expression)*
	;

// identifier, followed by member refs (dot ident), or method calls.
// NOTE: handleDotIdent() is called immediately after the first IDENT is recognized because
// the method looks a head to find keywords after DOT and turns them into identifiers.
identPrimary
    : i:identPrimaryBase { handleDotIdent(); }
			( options { greedy=true; } : DOT^ ( identifier { handleDotIdent(); }  | ELEMENTS | o:OBJECT { #o.setType(IDENT); } ) )*
			( options { greedy=true; } :
				( op:OPEN^ { #op.setType(METHOD_CALL);} e:exprList CLOSE! ) {
				    AST path = #e.getFirstChild();
				    if ( #i.getText().equalsIgnoreCase( "key" ) ) {
				        #identPrimary = #( [KEY], path );
				    }
				    else if ( #i.getText().equalsIgnoreCase( "value" ) ) {
				        #identPrimary = #( [VALUE], path );
				    }
				    else if ( #i.getText().equalsIgnoreCase( "entry" ) ) {
				        #identPrimary = #( [ENTRY], path );
				    }
				}
			)?
	// Also allow special 'aggregate functions' such as count(), avg(), etc.
	| aggregate
	;

identPrimaryBase
    : { validateSoftKeyword("treat") && LA(2) == OPEN }? castedIdentPrimaryBase
    | i:identifier
    ;

castedIdentPrimaryBase
    : i:IDENT! OPEN! p:path AS! a:path! CLOSE! { i.getText().equalsIgnoreCase("treat") }? {
        registerTreat( #p, #a );
    }
    ;

aggregate
	: ( SUM^ | AVG^ | MAX^ | MIN^ ) OPEN! ( additiveExpression | selectStatement ) CLOSE! { #aggregate.setType(AGGREGATE); }
	// Special case for count - It's 'parameters' can be keywords.
	|  COUNT^ OPEN! ( STAR { #STAR.setType(ROW_STAR); } | ( ( DISTINCT | ALL )? ( path | collectionExpr | NUM_INT | caseExpression ) ) ) CLOSE!
	|  collectionExpr
	;

//## collection: ( OPEN query CLOSE ) | ( 'elements'|'indices' OPEN path CLOSE );

collectionExpr
	: (ELEMENTS^ | INDICES^) OPEN! path CLOSE!
	;
                                           
// NOTE: compoundExpr can be a 'path' where the last token in the path is '.elements' or '.indicies'
compoundExpr
	: collectionExpr
	| path
	| { LA(1) == OPEN && LA(2) == CLOSE }? OPEN! CLOSE!
	| (OPEN! ( (expression (COMMA! expression)*) | subQuery ) CLOSE!)
	| parameter
	;

subQuery
	: queryRule
	{ #subQuery = #([QUERY,"query"], #subQuery); }
	;

exprList
{
   AST trimSpec = null;
}
	: (t:TRAILING {#trimSpec = #t;} | l:LEADING {#trimSpec = #l;} | b:BOTH {#trimSpec = #b;})?
	  		{ if(#trimSpec != null) #trimSpec.setType(IDENT); }
	  ( 
	  		expression ( (COMMA! expression)+ | FROM { #FROM.setType(IDENT); } expression | AS! identifier )? 
	  		| FROM { #FROM.setType(IDENT); } expression
	  )?
			{ #exprList = #([EXPR_LIST,"exprList"], #exprList); }
	;

constant
	: NUM_INT
	| NUM_FLOAT
	| NUM_LONG
	| NUM_DOUBLE
	| NUM_BIG_INTEGER
	| NUM_BIG_DECIMAL
	| QUOTED_STRING
	| NULL
	| TRUE
	| FALSE
	| EMPTY
	;

//## quantifiedExpression: 'exists' | ( expression 'in' ) | ( expression OP 'any' | 'some' ) collection;

//## compoundPath: path ( OPEN_BRACKET expression CLOSE_BRACKET ( '.' path )? )*;

//## path: identifier ( '.' identifier )*;

path
	: {firstPathTokenWeakKeywords();} identifier ( DOT^ { weakKeywords(); } identifier )*
	;

// Wraps the IDENT token from the lexer, in order to provide
// 'keyword as identifier' trickery.
identifier
	: IDENT
	exception
	catch [RecognitionException ex]
	{
		identifier_AST = handleIdentifierError(LT(1),ex);
	}
	;

// **** LEXER ******************************************************************

/**
 * Hibernate Query Language Lexer
 * <br>
 * This lexer provides the HQL parser with tokens.
 * @author Joshua Davis (pgmjsd@sourceforge.net)
 */
class HqlBaseLexer extends Lexer;

options {
	exportVocab=Hql;      // call the vocabulary "Hql"
	testLiterals = false;
	k=2; // needed for newline, and to distinguish '>' from '>='.
	// HHH-241 : Quoted strings don't allow unicode chars - This should fix it.
	charVocabulary='\u0000'..'\uFFFE';	// Allow any char but \uFFFF (16 bit -1, ANTLR's EOF character)
	caseSensitive = false;
	caseSensitiveLiterals = false;
}

// -- Declarations --
{
	// NOTE: The real implementations are in the subclass.
	protected void setPossibleID(boolean possibleID) {}
}

// -- Keywords --

EQ: '=';
LT: '<';
GT: '>';
SQL_NE: "<>";
NE: "!=" | "^=";
LE: "<=";
GE: ">=";

COMMA: ',';

OPEN: '(';
CLOSE: ')';
OPEN_BRACKET: '[';
CLOSE_BRACKET: ']';

CONCAT: "||";
PLUS: '+';
MINUS: '-';
STAR: '*';
DIV: '/';
MOD: '%';
COLON: ':';
PARAM: '?';

IDENT options { testLiterals=true; }
	: ID_START_LETTER ( ID_LETTER )*
		{
    		// Setting this flag allows the grammar to use keywords as identifiers, if necessary.
			setPossibleID(true);
		}
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

WS  :   (   ' '
		|   '\t'
		|   '\r' '\n' { newline(); }
		|   '\n'      { newline(); }
		|   '\r'      { newline(); }
		)
		{$setType(Token.SKIP);} //ignore this token
	;

//--- From the Java example grammar ---
// a numeric literal
NUM_INT
	{boolean isDecimal=false; Token t=null;}
	:   '.' {_ttype = DOT;}
			(	('0'..'9')+ (EXPONENT)? (f1:FLOAT_SUFFIX {t=f1;})?
				{
					if ( t != null && t.getText().toUpperCase().indexOf("BD")>=0) {
						_ttype = NUM_BIG_DECIMAL;
					}
					else if (t != null && t.getText().toUpperCase().indexOf('F')>=0) {
						_ttype = NUM_FLOAT;
					}
					else {
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
		|	('b''i') { _ttype = NUM_BIG_INTEGER; }

		// only check to see if it's a float if looks like decimal so far
		|	{isDecimal}?
			(   '.' ('0'..'9')* (EXPONENT)? (f2:FLOAT_SUFFIX {t=f2;})?
			|   EXPONENT (f3:FLOAT_SUFFIX {t=f3;})?
			|   f4:FLOAT_SUFFIX {t=f4;}
			)
			{
				if ( t != null && t.getText().toUpperCase().indexOf("BD")>=0) {
					_ttype = NUM_BIG_DECIMAL;
				}
				else if (t != null && t.getText().toUpperCase() .indexOf('F') >= 0) {
					_ttype = NUM_FLOAT;
				}
				else {
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
	:	'f'|'d'|'b''d'
	;

