header
{
package org.hibernate.hql.internal.antlr;

import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;
}

/**
 * Hibernate Query Language to SQL Tree Transform.<br>
 * This is a tree grammar that transforms an HQL AST into a intermediate SQL AST
 * with bindings to Hibernate interfaces (Queryable, etc.).  The Hibernate specific methods
 * are all implemented in the HqlSqlWalker subclass, allowing the ANTLR-generated class
 * to have only the minimum dependencies on the Hibernate code base.   This will also allow
 * the sub-class to be easily edited using an IDE (most IDE's don't support ANTLR).
 * <br>
 * <i>NOTE:</i> The java class is generated from hql-sql.g by ANTLR.
 * <i>DO NOT EDIT THE GENERATED JAVA SOURCE CODE.</i>
 * @author Joshua Davis (joshua@hibernate.org)
 */
class HqlSqlBaseWalker extends TreeParser;

options
{
	// Note: importVocab and exportVocab cause ANTLR to share the token type numbers between the
	// two grammars.  This means that the token type constants from the source tree are the same
	// as those in the target tree.  If this is not the case, tree translation can result in
	// token types from the *source* tree being present in the target tree.
	importVocab=Hql;        // import definitions from "Hql"
	exportVocab=HqlSql;     // Call the resulting definitions "HqlSql"
	buildAST=true;
}

tokens
{
	FROM_FRAGMENT;	// A fragment of SQL that represents a table reference in a FROM clause.
	IMPLIED_FROM;	// An implied FROM element.
	JOIN_FRAGMENT;	// A JOIN fragment.
	SELECT_CLAUSE;
	LEFT_OUTER;
	RIGHT_OUTER;
	ALIAS_REF;      // An IDENT that is a reference to an entity via it's alias.
	PROPERTY_REF;   // A DOT that is a reference to a property in an entity.
	SQL_TOKEN;      // A chunk of SQL that is 'rendered' already.
	SELECT_COLUMNS; // A chunk of SQL representing a bunch of select columns.
	SELECT_EXPR;    // A select expression, generated from a FROM element.
	THETA_JOINS;	// Root of theta join condition subtree.
	FILTERS;		// Root of the filters condition subtree.
	METHOD_NAME;    // An IDENT that is a method name.
	NAMED_PARAM;    // A named parameter (:foo).
	BOGUS;          // Used for error state detection, etc.
	RESULT_VARIABLE_REF;   // An IDENT that refers to result variable
	                       // (i.e, an alias for a select expression) 
}

// -- Declarations --
{
    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, HqlSqlBaseWalker.class.getName());

	private int level = 0;

	private boolean inSelect = false;
	private boolean inFunctionCall = false;
	private boolean inCase = false;
	private boolean inFrom = false;
	private boolean inCount = false;
	private boolean inCountDistinct = false;

	private int statementType;
	private String statementTypeName;
	// Note: currentClauseType tracks the current clause within the current
	// statement, regardless of level; currentTopLevelClauseType, on the other
	// hand, tracks the current clause within the top (or primary) statement.
	// Thus, currentTopLevelClauseType ignores the clauses from any subqueries.
	private int currentClauseType;
	private int currentTopLevelClauseType;
	private int currentStatementType;

	public final boolean isSubQuery() {
		return level > 1;
	}

	public final boolean isInFrom() {
		return inFrom;
	}

	public final boolean isInFunctionCall() {
		return inFunctionCall;
	}
	
	public final boolean isInSelect() {
		return inSelect;
	}

	public final boolean isInCase() {
		return inCase;
	}

    public final boolean isInCount() {
        return inCount;
    }

    public final boolean isInCountDistinct() {
        return inCountDistinct;
    }

	public final int getStatementType() {
		return statementType;
	}

	public final int getCurrentClauseType() {
		return currentClauseType;
	}

	public final int getCurrentTopLevelClauseType() {
		return currentTopLevelClauseType;
	}

	public final int getCurrentStatementType() {
		return currentStatementType;
	}

	public final boolean isComparativeExpressionClause() {
		// Note: once we add support for "JOIN ... ON ...",
		// the ON clause needs to get included here
	    return getCurrentClauseType() == WHERE ||
	            getCurrentClauseType() == WITH ||
	            isInCase();
	}

	public final boolean isSelectStatement() {
		return statementType == SELECT;
	}

	private void beforeStatement(String statementName, int statementType) {
		inFunctionCall = false;
		level++;
		if ( level == 1 ) {
			this.statementTypeName = statementName;
			this.statementType = statementType;
		}
		currentStatementType = statementType;
		LOG.debugf("%s << begin [level=%s, statement=%s]", statementName, level, this.statementTypeName);
	}

	private void beforeStatementCompletion(String statementName) {
        LOG.debugf("%s : finishing up [level=%s, statement=%s]", statementName, level, this.statementTypeName);
	}

	private void afterStatementCompletion(String statementName) {
        LOG.debugf("%s >> end [level=%s, statement=%s]", statementName, level, this.statementTypeName);
		level--;
	}

	private void handleClauseStart(int clauseType) {
		currentClauseType = clauseType;
		if ( level == 1 ) {
			currentTopLevelClauseType = clauseType;
		}
	}

	///////////////////////////////////////////////////////////////////////////
	// NOTE: The real implementations for the following are in the subclass.

	protected void evaluateAssignment(AST eq) throws SemanticException { }
	
	/** Pre-process the from clause input tree. **/
	protected void prepareFromClauseInputTree(AST fromClauseInput) {}

	/** Sets the current 'FROM' context. **/
	protected void pushFromClause(AST fromClause,AST inputFromNode) {}

	protected AST createFromElement(String path,AST alias,AST propertyFetch) throws SemanticException {
		return null;
	}

	protected void createFromJoinElement(AST path,AST alias,int joinType,AST fetch,AST propertyFetch,AST with) throws SemanticException {}

	protected AST createFromFilterElement(AST filterEntity,AST alias) throws SemanticException	{
		return null;
	}

	protected void processQuery(AST select,AST query) throws SemanticException { }

	protected void postProcessUpdate(AST update) throws SemanticException { }

	protected void postProcessDelete(AST delete) throws SemanticException { }

	protected void postProcessInsert(AST insert) throws SemanticException { }

	protected void beforeSelectClause() throws SemanticException { }

	protected void processIndex(AST indexOp) throws SemanticException { }

	protected void processConstant(AST constant) throws SemanticException { }

	protected void processBoolean(AST constant) throws SemanticException { }

	protected void processNumericLiteral(AST literal) throws SemanticException { }

	protected void resolve(AST node) throws SemanticException { }

	protected void resolveSelectExpression(AST dotNode) throws SemanticException { }

	protected void processFunction(AST functionCall,boolean inSelect) throws SemanticException { }

	protected void processCastFunction(AST functionCall,boolean inSelect) throws SemanticException { }

	protected void processAggregation(AST node, boolean inSelect) throws SemanticException { }

	protected void processConstructor(AST constructor) throws SemanticException { }

	protected AST generateNamedParameter(AST delimiterNode, AST nameNode) throws SemanticException {
		return #( [NAMED_PARAM, nameNode.getText()] );
	}

	protected AST generatePositionalParameter(AST inputNode) throws SemanticException {
		return #( [PARAM, "?"] );
	}

	protected void lookupAlias(AST ident) throws SemanticException { }

	protected void setAlias(AST selectExpr, AST ident) { }

	protected boolean isOrderExpressionResultVariableRef(AST ident) throws SemanticException {
		return false;
	}

	protected void handleResultVariableRef(AST resultVariableRef) throws SemanticException {
	}

	protected AST lookupProperty(AST dot,boolean root,boolean inSelect) throws SemanticException {
		return dot;
	}

	protected boolean isNonQualifiedPropertyRef(AST ident) { return false; }

	protected AST lookupNonQualifiedProperty(AST property) throws SemanticException { return property; }

	protected void setImpliedJoinType(int joinType) { }

	protected AST createIntoClause(String path, AST propertySpec) throws SemanticException {
		return null;
	};

	protected void prepareVersioned(AST updateNode, AST versionedNode) throws SemanticException {}

	protected void prepareLogicOperator(AST operator) throws SemanticException { }

	protected void prepareArithmeticOperator(AST operator) throws SemanticException { }

    protected void processMapComponentReference(AST node) throws SemanticException { }

	protected void validateMapPropertyExpression(AST node) throws SemanticException { }
}

// The main statement rule.
statement
	: selectStatement | updateStatement | deleteStatement | insertStatement
	;

selectStatement
	: query
	;

// Cannot use just the fromElement rule here in the update and delete queries
// because fromElement essentially relies on a FromClause already having been
// built :(
updateStatement!
	: #( u:UPDATE { beforeStatement( "update", UPDATE ); } (v:VERSIONED)? f:fromClause s:setClause (w:whereClause)? ) {
		#updateStatement = #(#u, #f, #s, #w);
		beforeStatementCompletion( "update" );
		prepareVersioned( #updateStatement, #v );
		postProcessUpdate( #updateStatement );
		afterStatementCompletion( "update" );
	}
	;

deleteStatement
	: #( DELETE { beforeStatement( "delete", DELETE ); } fromClause (whereClause)? ) {
		beforeStatementCompletion( "delete" );
		postProcessDelete( #deleteStatement );
		afterStatementCompletion( "delete" );
	}
	;

insertStatement
	// currently only "INSERT ... SELECT ..." statements supported;
	// do we also need support for "INSERT ... VALUES ..."?
	//
	: #( INSERT { beforeStatement( "insert", INSERT ); } intoClause query ) {
		beforeStatementCompletion( "insert" );
		postProcessInsert( #insertStatement );
		afterStatementCompletion( "insert" );
	}
	;

intoClause! {
		String p = null;
	}
	: #( INTO { handleClauseStart( INTO ); } (p=path) ps:insertablePropertySpec ) {
		#intoClause = createIntoClause(p, ps);
	}
	;

insertablePropertySpec
	: #( RANGE (IDENT)+ )
	;

setClause
	: #( SET { handleClauseStart( SET ); } (assignment)* )
	;

assignment
	// Note: the propertyRef here needs to be resolved
	// *before* we evaluate the newValue rule...
	: #( EQ (p:propertyRef) { resolve(#p); } (newValue) ) {
		evaluateAssignment( #assignment );
	}
	;

// For now, just use expr.  Revisit after ejb3 solidifies this.
newValue
	: expr | query
	;

// The query / subquery rule. Pops the current 'from node' context 
// (list of aliases).
query!
	: #( QUERY { beforeStatement( "select", SELECT ); }
			// The first phase places the FROM first to make processing the SELECT simpler.
			#(SELECT_FROM
				f:fromClause
				(s:selectClause)?
			)
			(w:whereClause)?
			(g:groupClause)?
			(o:orderClause)?
		) {
		// Antlr note: #x_in refers to the input AST, #x refers to the output AST
		#query = #([SELECT,"SELECT"], #s, #f, #w, #g, #o);
		beforeStatementCompletion( "select" );
		processQuery( #s, #query );
		afterStatementCompletion( "select" );
	}
	;

orderClause
	: #(ORDER { handleClauseStart( ORDER ); } orderExprs)
	;

orderExprs
	: orderExpr ( ASCENDING | DESCENDING )? ( nullOrdering )? (orderExprs)?
	;

nullOrdering
    : NULLS nullPrecedence
    ;

nullPrecedence
    : FIRST
    | LAST
    ;

orderExpr
	: { isOrderExpressionResultVariableRef( _t ) }? resultVariableRef
	| expr
	;

resultVariableRef!
	: i:identifier {
		// Create a RESULT_VARIABLE_REF node instead of an IDENT node.
		#resultVariableRef = #([RESULT_VARIABLE_REF, i.getText()]);
		handleResultVariableRef(#resultVariableRef);
	}
	;

groupClause
	: #(GROUP { handleClauseStart( GROUP ); } (expr)+ ( #(HAVING logicalExpr) )? )
	;

selectClause!
	: #(SELECT { handleClauseStart( SELECT ); beforeSelectClause(); } (d:DISTINCT)? x:selectExprList ) {
		#selectClause = #([SELECT_CLAUSE,"{select clause}"], #d, #x);
	}
	;

selectExprList {
		boolean oldInSelect = inSelect;
		inSelect = true;
	}
	: ( selectExpr | aliasedSelectExpr )+ {
		inSelect = oldInSelect;
	}
	;

aliasedSelectExpr!
	: #(AS se:selectExpr i:identifier) {
		setAlias(#se,#i);
		#aliasedSelectExpr = #se;
	}
	;

selectExpr
	: p:propertyRef					{ resolveSelectExpression(#p); }
	| #(ALL ar2:aliasRef) 			{ resolveSelectExpression(#ar2); #selectExpr = #ar2; }
	| #(OBJECT ar3:aliasRef)		{ resolveSelectExpression(#ar3); #selectExpr = #ar3; }
	| con:constructor 				{ processConstructor(#con); }
	| functionCall
	| count
	| collectionFunction			// elements() or indices()
	| literal
	| arithmeticExpr
	| logicalExpr
	| parameter
	| query
	;

count
    : #(COUNT  { inCount = true; } ( DISTINCT { inCountDistinct = true; } | ALL )? ( aggregateExpr | ROW_STAR ) ) {
        inCount = false;
        inCountDistinct = false;
    }
    ;

constructor
	{ String className = null; }
	: #(CONSTRUCTOR className=path ( selectExpr | aliasedSelectExpr )* )
	;

aggregateExpr
	: expr //p:propertyRef { resolve(#p); }
	| collectionFunction
	;

// Establishes the list of aliases being used by this query.
fromClause {
		// NOTE: This references the INPUT AST! (see http://www.antlr.org/doc/trees.html#Action%20Translation)
		// the ouput AST (#fromClause) has not been built yet.
		prepareFromClauseInputTree(#fromClause_in);
	}
	: #(f:FROM { pushFromClause(#fromClause,f); handleClauseStart( FROM ); } fromElementList )
	;

fromElementList {
		boolean oldInFrom = inFrom;
		inFrom = true;
		}
	: (fromElement)+ {
		inFrom = oldInFrom;
		}
	;

fromElement! {
	String p = null;
	}
	// A simple class name, alias element.
	: #(RANGE p=path (a:ALIAS)? (pf:FETCH)? ) {
		#fromElement = createFromElement(p,a, pf);
	}
	| je:joinElement {
		#fromElement = #je;
	}
	// A from element created due to filter compilation
	| fe:FILTER_ENTITY a3:ALIAS {
		#fromElement = createFromFilterElement(fe,a3);
	}
	;

joinElement! {
		int j = INNER;
	}
	// A from element with a join.  This time, the 'path' should be treated as an AST
	// and resolved (like any path in a WHERE clause).   Make sure all implied joins
	// generated by the property ref use the join type, if it was specified.
	: #(JOIN (j=joinType { setImpliedJoinType(j); } )? (f:FETCH)? ref:propertyRef (a:ALIAS)? (pf:FETCH)? (with:WITH)? ) {
		//createFromJoinElement(#ref,a,j,f, pf);
		createFromJoinElement(#ref,a,j,f, pf, with);
		setImpliedJoinType(INNER);	// Reset the implied join type.
	}
	;

// Returns an node type integer that represents the join type
// tokens.
joinType returns [int j] {
	j = INNER;
	}
	: ( (left:LEFT | right:RIGHT) (outer:OUTER)? ) {
		if (left != null)       j = LEFT_OUTER;
		else if (right != null) j = RIGHT_OUTER;
		else if (outer != null) j = RIGHT_OUTER;
	}
	| FULL {
		j = FULL;
	}
	| INNER {
		j = INNER;
	}
	;

// Matches a path and returns the normalized string for the path (usually
// fully qualified a class name).
path returns [String p] {
	p = "???";
	String x = "?x?";
	}
	: a:identifier { p = a.getText(); }
	| #(DOT x=path y:identifier) {
			StringBuilder buf = new StringBuilder();
			buf.append(x).append(".").append(y.getText());
			p = buf.toString();
		}
	;

// Returns a path as a single identifier node.
pathAsIdent {
    String text = "?text?";
    }
    : text=path {
        #pathAsIdent = #([IDENT,text]);
    }
    ;

withClause
	// Note : this is used internally from the HqlSqlWalker to
	// parse the node recognized with the with keyword earlier.
	// Done this way because it relies on the join it "qualifies"
	// already having been processed, which would not be the case
	// if withClause was simply referenced from the joinElement
	// rule during recognition...
	: #(w:WITH { handleClauseStart( WITH ); } b:logicalExpr ) {
		#withClause = #(w , #b);
	}
	;

whereClause
	: #(w:WHERE { handleClauseStart( WHERE ); } b:logicalExpr ) {
		// Use the *output* AST for the boolean expression!
		#whereClause = #(w , #b);
	}
	;

logicalExpr
	: #(AND logicalExpr logicalExpr)
	| #(OR logicalExpr logicalExpr)
	| #(NOT logicalExpr)
	| comparisonExpr
	;

// TODO: Add any other comparison operators here.
comparisonExpr
	:
	( #(EQ exprOrSubquery exprOrSubquery)
	| #(NE exprOrSubquery exprOrSubquery)
	| #(LT exprOrSubquery exprOrSubquery)
	| #(GT exprOrSubquery exprOrSubquery)
	| #(LE exprOrSubquery exprOrSubquery)
	| #(GE exprOrSubquery exprOrSubquery)
	| #(LIKE exprOrSubquery expr ( #(ESCAPE expr) )? )
	| #(NOT_LIKE exprOrSubquery expr ( #(ESCAPE expr) )? )
	| #(BETWEEN exprOrSubquery exprOrSubquery exprOrSubquery)
	| #(NOT_BETWEEN exprOrSubquery exprOrSubquery exprOrSubquery)
	| #(IN exprOrSubquery inRhs )
	| #(NOT_IN exprOrSubquery inRhs )
	| #(IS_NULL exprOrSubquery)
	| #(IS_NOT_NULL exprOrSubquery)
//	| #(IS_TRUE expr)
//	| #(IS_FALSE expr)
	| #(EXISTS ( expr | collectionFunctionOrSubselect ) )
	) {
	    prepareLogicOperator( #comparisonExpr );
	}
	;

inRhs
	: #(IN_LIST ( collectionFunctionOrSubselect | ( (expr)* ) ) )
	;

exprOrSubquery
	: expr
	| query
	| #(ANY collectionFunctionOrSubselect)
	| #(ALL collectionFunctionOrSubselect)
	| #(SOME collectionFunctionOrSubselect)
	;
	
collectionFunctionOrSubselect
	: collectionFunction
	| query
	;
	
expr
	: ae:addrExpr [ true ] { resolve(#ae); }	// Resolve the top level 'address expression'
	| #( VECTOR_EXPR (expr)* )
	| constant
	| arithmeticExpr
	| functionCall							// Function call, not in the SELECT clause.
	| parameter
	| count										// Count, not in the SELECT clause.
	;

arithmeticExpr
    : #(PLUS exprOrSubquery exprOrSubquery)         { prepareArithmeticOperator( #arithmeticExpr ); }
    | #(MINUS exprOrSubquery exprOrSubquery)        { prepareArithmeticOperator( #arithmeticExpr ); }
    | #(DIV exprOrSubquery exprOrSubquery)          { prepareArithmeticOperator( #arithmeticExpr ); }
    | #(MOD exprOrSubquery exprOrSubquery)          { prepareArithmeticOperator( #arithmeticExpr ); }
    | #(STAR exprOrSubquery exprOrSubquery)         { prepareArithmeticOperator( #arithmeticExpr ); }
//	| #(CONCAT expr (expr)+ )   { prepareArithmeticOperator( #arithmeticExpr ); }
	| #(UNARY_MINUS expr)       { prepareArithmeticOperator( #arithmeticExpr ); }
	| caseExpr
	;

caseExpr
	: #(CASE { inCase = true; } (#(WHEN logicalExpr expr))+ (#(ELSE expr))?) { inCase = false; }
	| #(CASE2 { inCase = true; } expr (#(WHEN expr expr))+ (#(ELSE expr))?) { inCase = false; }
	;

//TODO: I don't think we need this anymore .. how is it different to 
//      maxelements, etc, which are handled by functionCall
collectionFunction
	: #(e:ELEMENTS {inFunctionCall=true;} p1:propertyRef { resolve(#p1); } ) 
		{ processFunction(#e,inSelect); } {inFunctionCall=false;}
	| #(i:INDICES {inFunctionCall=true;} p2:propertyRef { resolve(#p2); } ) 
		{ processFunction(#i,inSelect); } {inFunctionCall=false;}
	;

functionCall
	: #(METHOD_CALL  {inFunctionCall=true;} pathAsIdent ( #(EXPR_LIST (exprOrSubquery)* ) )? ) {
        processFunction( #functionCall, inSelect );
        inFunctionCall=false;
    }
    | #(CAST {inFunctionCall=true;} exprOrSubquery pathAsIdent) {
    	processCastFunction( #functionCall, inSelect );
        inFunctionCall=false;
    }
	| #(AGGREGATE aggregateExpr )
	;

constant
	: literal
	| NULL
	| TRUE { processBoolean(#constant); } 
	| FALSE { processBoolean(#constant); }
	| JAVA_CONSTANT
	;

literal
	: NUM_INT { processNumericLiteral( #literal ); }
	| NUM_LONG { processNumericLiteral( #literal ); }
	| NUM_FLOAT { processNumericLiteral( #literal ); }
	| NUM_DOUBLE { processNumericLiteral( #literal ); }
	| NUM_BIG_INTEGER { processNumericLiteral( #literal ); }
	| NUM_BIG_DECIMAL { processNumericLiteral( #literal ); }
	| QUOTED_STRING
	;

identifier
	: (IDENT | WEIRD_IDENT)
	;

addrExpr! [ boolean root ]
	: #(d:DOT lhs:addrExprLhs rhs:propertyName )	{
		// This gives lookupProperty() a chance to transform the tree 
		// to process collection properties (.elements, etc).
		#addrExpr = #(#d, #lhs, #rhs);
		#addrExpr = lookupProperty(#addrExpr,root,false);
	}
	| #(i:INDEX_OP lhs2:addrExprLhs rhs2:expr)	{
		#addrExpr = #(#i, #lhs2, #rhs2);
		processIndex(#addrExpr);
	}
	| mcr:mapComponentReference {
	    #addrExpr = #mcr;
	}
	| p:identifier {
//		#addrExpr = #p;
//		resolve(#addrExpr);
		// In many cases, things other than property-refs are recognized
		// by this addrExpr rule.  Some of those I have seen:
		//  1) select-clause from-aliases
		//  2) sql-functions
		if ( isNonQualifiedPropertyRef(#p) ) {
			#addrExpr = lookupNonQualifiedProperty(#p);
		}
		else {
			resolve(#p);
			#addrExpr = #p;
		}
	}
	;

addrExprLhs
	: addrExpr [ false ]
	;

propertyName
	: identifier
	| CLASS
	| ELEMENTS
	| INDICES
	;

propertyRef!
	: mcr:mapComponentReference {
	    resolve( #mcr );
	    #propertyRef = #mcr;
	}
	| #(d:DOT lhs:propertyRefLhs rhs:propertyName )	{
		// This gives lookupProperty() a chance to transform the tree to process collection properties (.elements, etc).
		#propertyRef = #(#d, #lhs, #rhs);
		#propertyRef = lookupProperty(#propertyRef,false,true);
	}
	|
	p:identifier {
		// In many cases, things other than property-refs are recognized
		// by this propertyRef rule.  Some of those I have seen:
		//  1) select-clause from-aliases
		//  2) sql-functions
		if ( isNonQualifiedPropertyRef(#p) ) {
			#propertyRef = lookupNonQualifiedProperty(#p);
		}
		else {
			resolve(#p);
			#propertyRef = #p;
		}
	}
	;

propertyRefLhs
	: propertyRef
	;

aliasRef!
	: i:identifier {
		#aliasRef = #([ALIAS_REF,i.getText()]);	// Create an ALIAS_REF node instead of an IDENT node.
		lookupAlias(#aliasRef);
		}
	;

mapComponentReference
    : #( KEY mapPropertyExpression )
    | #( VALUE mapPropertyExpression )
    | #( ENTRY mapPropertyExpression )
    ;

mapPropertyExpression
    : e:expr {
        validateMapPropertyExpression( #e );
    }
    ;

parameter!
	: #(c:COLON a:identifier) {
			// Create a NAMED_PARAM node instead of (COLON IDENT).
			#parameter = generateNamedParameter( c, a );
//			#parameter = #([NAMED_PARAM,a.getText()]);
//			namedParameter(#parameter);
		}
	| #(p:PARAM (n:NUM_INT)?) {
			if ( n != null ) {
				// An ejb3-style "positional parameter", which we handle internally as a named-param
				#parameter = generateNamedParameter( p, n );
//				#parameter = #([NAMED_PARAM,n.getText()]);
//				namedParameter(#parameter);
			}
			else {
				#parameter = generatePositionalParameter( p );
//				#parameter = #([PARAM,"?"]);
//				positionalParameter(#parameter);
			}
		}
	;

numericInteger
	: NUM_INT
	;
