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
package org.hibernate.hql.internal.classic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.MappingException;
import org.hibernate.QueryException;
import org.hibernate.engine.internal.JoinSequence;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.persister.collection.CollectionPropertyNames;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.InFragment;
import org.hibernate.type.EntityType;
import org.hibernate.type.LiteralType;
import org.hibernate.type.Type;

/**
 * Parses the where clause of a hibernate query and translates it to an
 * SQL where clause.
 */

// We should reengineer this class so that, rather than the current ad -
// hoc linear approach to processing a stream of tokens, we instead
// build up a tree of expressions.

// We would probably refactor to have LogicParser (builds a tree of simple
// expressions connected by and, or, not), ExpressionParser (translates
// from OO terms like foo, foo.Bar, foo.Bar.Baz to SQL terms like
// FOOS.ID, FOOS.BAR_ID, etc) and PathExpressionParser (which does much
// the same thing it does now)

public class WhereParser implements Parser {

	private final PathExpressionParser pathExpressionParser;

	{
		pathExpressionParser = new PathExpressionParser();
		pathExpressionParser.setUseThetaStyleJoin( true ); //Need this, since join condition can appear inside parens!
	}

	private static final Set EXPRESSION_TERMINATORS = new HashSet();   //tokens that close a sub expression
	private static final Set EXPRESSION_OPENERS = new HashSet();       //tokens that open a sub expression
	private static final Set BOOLEAN_OPERATORS = new HashSet();        //tokens that would indicate a sub expression is a boolean expression
	private static final Map NEGATIONS = new HashMap();

	static {
		EXPRESSION_TERMINATORS.add( "and" );
		EXPRESSION_TERMINATORS.add( "or" );
		EXPRESSION_TERMINATORS.add( ")" );
		//expressionTerminators.add(","); // deliberately excluded

		EXPRESSION_OPENERS.add( "and" );
		EXPRESSION_OPENERS.add( "or" );
		EXPRESSION_OPENERS.add( "(" );
		//expressionOpeners.add(","); // deliberately excluded

		BOOLEAN_OPERATORS.add( "<" );
		BOOLEAN_OPERATORS.add( "=" );
		BOOLEAN_OPERATORS.add( ">" );
		BOOLEAN_OPERATORS.add( "#" );
		BOOLEAN_OPERATORS.add( "~" );
		BOOLEAN_OPERATORS.add( "like" );
		BOOLEAN_OPERATORS.add( "ilike" );
		BOOLEAN_OPERATORS.add( "regexp" );
		BOOLEAN_OPERATORS.add( "rlike" );
		BOOLEAN_OPERATORS.add( "is" );
		BOOLEAN_OPERATORS.add( "in" );
		BOOLEAN_OPERATORS.add( "any" );
		BOOLEAN_OPERATORS.add( "some" );
		BOOLEAN_OPERATORS.add( "all" );
		BOOLEAN_OPERATORS.add( "exists" );
		BOOLEAN_OPERATORS.add( "between" );
		BOOLEAN_OPERATORS.add( "<=" );
		BOOLEAN_OPERATORS.add( ">=" );
		BOOLEAN_OPERATORS.add( "=>" );
		BOOLEAN_OPERATORS.add( "=<" );
		BOOLEAN_OPERATORS.add( "!=" );
		BOOLEAN_OPERATORS.add( "<>" );
		BOOLEAN_OPERATORS.add( "!#" );
		BOOLEAN_OPERATORS.add( "!~" );
		BOOLEAN_OPERATORS.add( "!<" );
		BOOLEAN_OPERATORS.add( "!>" );
		BOOLEAN_OPERATORS.add( "is not" );
		BOOLEAN_OPERATORS.add( "not like" );
		BOOLEAN_OPERATORS.add( "not ilike" );
		BOOLEAN_OPERATORS.add( "not regexp" );
		BOOLEAN_OPERATORS.add( "not rlike" );
		BOOLEAN_OPERATORS.add( "not in" );
		BOOLEAN_OPERATORS.add( "not between" );
		BOOLEAN_OPERATORS.add( "not exists" );

		NEGATIONS.put( "and", "or" );
		NEGATIONS.put( "or", "and" );
		NEGATIONS.put( "<", ">=" );
		NEGATIONS.put( "=", "<>" );
		NEGATIONS.put( ">", "<=" );
		NEGATIONS.put( "#", "!#" );
		NEGATIONS.put( "~", "!~" );
		NEGATIONS.put( "like", "not like" );
		NEGATIONS.put( "ilike", "not ilike" );
		NEGATIONS.put( "regexp", "not regexp" );
		NEGATIONS.put( "rlike", "not rlike" );
		NEGATIONS.put( "is", "is not" );
		NEGATIONS.put( "in", "not in" );
		NEGATIONS.put( "exists", "not exists" );
		NEGATIONS.put( "between", "not between" );
		NEGATIONS.put( "<=", ">" );
		NEGATIONS.put( ">=", "<" );
		NEGATIONS.put( "=>", "<" );
		NEGATIONS.put( "=<", ">" );
		NEGATIONS.put( "!=", "=" );
		NEGATIONS.put( "<>", "=" );
		NEGATIONS.put( "!#", "#" );
		NEGATIONS.put( "!~", "~" );
		NEGATIONS.put( "!<", "<" );
		NEGATIONS.put( "!>", ">" );
		NEGATIONS.put( "is not", "is" );
		NEGATIONS.put( "not like", "like" );
		NEGATIONS.put( "not ilike", "ilike" );
		NEGATIONS.put( "not regexp", "regexp" );
		NEGATIONS.put( "not rlike", "rlike" );
		NEGATIONS.put( "not in", "in" );
		NEGATIONS.put( "not between", "between" );
		NEGATIONS.put( "not exists", "exists" );

	}
	// Handles things like:
	// a and b or c
	// a and ( b or c )
	// not a and not b
	// not ( a and b )
	// x between y and z            (overloaded "and")
	// x in ( a, b, c )             (overloaded brackets)
	// not not a
	// a is not null                (overloaded "not")
	// etc......
	// and expressions like
	// foo = bar                    (maps to: foo.id = bar.id)
	// foo.Bar = 'foo'              (maps to: foo.bar = 'foo')
	// foo.Bar.Baz = 1.0            (maps to: foo.bar = bar.id and bar.baz = 1.0)
	// 1.0 = foo.Bar.Baz            (maps to: bar.baz = 1.0 and foo.Bar = bar.id)
	// foo.Bar.Baz = a.B.C          (maps to: bar.Baz = b.C and foo.Bar = bar.id and a.B = b.id)
	// foo.Bar.Baz + a.B.C          (maps to: bar.Baz + b.C and foo.Bar = bar.id and a.B = b.id)
	// ( foo.Bar.Baz + 1.0 ) < 2.0  (maps to: ( bar.Baz + 1.0 ) < 2.0 and foo.Bar = bar.id)

	private boolean betweenSpecialCase;       //Inside a BETWEEN ... AND ... expression
	private boolean negated;

	private boolean inSubselect;
	private int bracketsSinceSelect;
	private StringBuilder subselect;

	private boolean expectingPathContinuation;
	private int expectingIndex;

	// The following variables are stacks that keep information about each subexpression
	// in the list of nested subexpressions we are currently processing.

	private LinkedList<Boolean> nots = new LinkedList<Boolean>();           //were an odd or even number of NOTs encountered
	private LinkedList joins = new LinkedList();          //the join string built up by compound paths inside this expression
	private LinkedList<Boolean> booleanTests = new LinkedList<Boolean>();   //a flag indicating if the subexpression is known to be boolean

	private String getElementName(PathExpressionParser.CollectionElement element, QueryTranslatorImpl q) throws QueryException {
		String name;
		if ( element.isOneToMany ) {
			name = element.alias;
		}
		else {
			Type type = element.elementType;
			if ( type.isEntityType() ) { //ie. a many-to-many
				String entityName = ( ( EntityType ) type ).getAssociatedEntityName();
				name = pathExpressionParser.continueFromManyToMany( entityName, element.elementColumns, q );
			}
			else {
				throw new QueryException( "illegally dereferenced collection element" );
			}
		}
		return name;
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		String lcToken = token.toLowerCase();

		//Cope with [,]
		if ( token.equals( "[" ) && !expectingPathContinuation ) {
			expectingPathContinuation = false;
			if ( expectingIndex == 0 ) throw new QueryException( "unexpected [" );
			return;
		}
		else if ( token.equals( "]" ) ) {
			expectingIndex--;
			expectingPathContinuation = true;
			return;
		}

		//Cope with a continued path expression (ie. ].baz)
		if ( expectingPathContinuation ) {
			boolean pathExpressionContinuesFurther = continuePathExpression( token, q );
			if ( pathExpressionContinuesFurther ) return; //NOTE: early return
		}

		//Cope with a subselect
		if ( !inSubselect && ( lcToken.equals( "select" ) || lcToken.equals( "from" ) ) ) {
			inSubselect = true;
			subselect = new StringBuilder( 20 );
		}
		if ( inSubselect && token.equals( ")" ) ) {
			bracketsSinceSelect--;

			if ( bracketsSinceSelect == -1 ) {
				QueryTranslatorImpl subq = new QueryTranslatorImpl(
				        subselect.toString(),
						q.getEnabledFilters(),
						q.getFactory()
				);
				try {
					subq.compile( q );
				}
				catch ( MappingException me ) {
					throw new QueryException( "MappingException occurred compiling subquery", me );
				}
				appendToken( q, subq.getSQLString() );
				inSubselect = false;
				bracketsSinceSelect = 0;
			}
		}
		if ( inSubselect ) {
			if ( token.equals( "(" ) ) bracketsSinceSelect++;
			subselect.append( token ).append( ' ' );
			return;
		}

		//Cope with special cases of AND, NOT, ()
		specialCasesBefore( lcToken );

		//Close extra brackets we opened
		if ( !betweenSpecialCase && EXPRESSION_TERMINATORS.contains( lcToken ) ) {
			closeExpression( q, lcToken );
		}

		//take note when this is a boolean expression
		if ( BOOLEAN_OPERATORS.contains( lcToken ) ) {
			booleanTests.removeLast();
			booleanTests.addLast( Boolean.TRUE );
		}

		if ( lcToken.equals( "not" ) ) {
			nots.addLast(  !(  nots.removeLast() ) );
			negated = !negated;
			return; //NOTE: early return
		}

		//process a token, mapping OO path expressions to SQL expressions
		doToken( token, q );

		//Open any extra brackets we might need.
		if ( !betweenSpecialCase && EXPRESSION_OPENERS.contains( lcToken ) ) {
			openExpression( q, lcToken );
		}

		//Cope with special cases of AND, NOT, )
		specialCasesAfter( lcToken );

	}

	public void start(QueryTranslatorImpl q) throws QueryException {
		token( "(", q );
	}

	public void end(QueryTranslatorImpl q) throws QueryException {
		if ( expectingPathContinuation ) {
			expectingPathContinuation = false;
			PathExpressionParser.CollectionElement element = pathExpressionParser.lastCollectionElement();
			if ( element.elementColumns.length != 1 ) throw new QueryException( "path expression ended in composite collection element" );
			appendToken( q, element.elementColumns[0] );
			addToCurrentJoin( element );
		}
		token( ")", q );
	}

	private void closeExpression(QueryTranslatorImpl q, String lcToken) {
		if ( booleanTests.removeLast() ) { //it was a boolean expression

			if ( booleanTests.size() > 0 ) {
				// the next one up must also be
				booleanTests.removeLast();
				booleanTests.addLast( Boolean.TRUE );
			}

			// Add any joins
			appendToken( q, ( joins.removeLast() ).toString() );

		}
		else {
			StringBuilder join = ( StringBuilder ) joins.removeLast();
			( ( StringBuilder ) joins.getLast() ).append( join.toString() );
		}

		if ( nots.removeLast() ) negated = !negated;

		if ( !")".equals( lcToken ) ) appendToken( q, ")" );
	}

	private void openExpression(QueryTranslatorImpl q, String lcToken) {
		nots.addLast( Boolean.FALSE );
		booleanTests.addLast( Boolean.FALSE );
		joins.addLast( new StringBuilder() );
		if ( !"(".equals( lcToken ) ) appendToken( q, "(" );
	}

	private void preprocess(String token, QueryTranslatorImpl q) throws QueryException {
		// ugly hack for cases like "elements(foo.bar.collection)"
		// (multi-part path expression ending in elements or indices)
		String[] tokens = StringHelper.split( ".", token, true );
		if (
				tokens.length > 5 &&
				( CollectionPropertyNames.COLLECTION_ELEMENTS.equals( tokens[tokens.length - 1] )
				|| CollectionPropertyNames.COLLECTION_INDICES.equals( tokens[tokens.length - 1] ) )
		) {
			pathExpressionParser.start( q );
			for ( int i = 0; i < tokens.length - 3; i++ ) {
				pathExpressionParser.token( tokens[i], q );
			}
			pathExpressionParser.token( null, q );
			pathExpressionParser.end( q );
			addJoin( pathExpressionParser.getWhereJoin(), q );
			pathExpressionParser.ignoreInitialJoin();
		}
	}

	private void doPathExpression(String token, QueryTranslatorImpl q) throws QueryException {

		preprocess( token, q );

		StringTokenizer tokens = new StringTokenizer( token, ".", true );
		pathExpressionParser.start( q );
		while ( tokens.hasMoreTokens() ) {
			pathExpressionParser.token( tokens.nextToken(), q );
		}
		pathExpressionParser.end( q );
		if ( pathExpressionParser.isCollectionValued() ) {
			openExpression( q, "" );
			appendToken( q, pathExpressionParser.getCollectionSubquery( q.getEnabledFilters() ) );
			closeExpression( q, "" );
			// this is ugly here, but needed because its a subquery
			q.addQuerySpaces( q.getCollectionPersister( pathExpressionParser.getCollectionRole() ).getCollectionSpaces() );
		}
		else {
			if ( pathExpressionParser.isExpectingCollectionIndex() ) {
				expectingIndex++;
			}
			else {
				addJoin( pathExpressionParser.getWhereJoin(), q );
				appendToken( q, pathExpressionParser.getWhereColumn() );
			}
		}
	}

	private void addJoin(JoinSequence joinSequence, QueryTranslatorImpl q) throws QueryException {
		//JoinFragment fromClause = q.createJoinFragment(true);
		//fromClause.addJoins( join.toJoinFragment().toFromFragmentString(), StringHelper.EMPTY_STRING );
		q.addFromJoinOnly( pathExpressionParser.getName(), joinSequence );
		try {
			addToCurrentJoin( joinSequence.toJoinFragment( q.getEnabledFilters(), true ).toWhereFragmentString() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	private void doToken(String token, QueryTranslatorImpl q) throws QueryException {
		if ( q.isName( StringHelper.root( token ) ) ) { //path expression
			doPathExpression( q.unalias( token ), q );
		}
		else if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) { //named query parameter
			q.addNamedParameter( token.substring( 1 ) );
			appendToken( q, "?" );
		}
		else {
			Queryable persister = q.getEntityPersisterUsingImports( token );
			if ( persister != null ) { // the name of a class
				final String discrim = persister.getDiscriminatorSQLValue();
				if ( InFragment.NULL.equals(discrim) || InFragment.NOT_NULL.equals(discrim) ) {
					throw new QueryException( "subclass test not allowed for null or not null discriminator" );
				}
				else {
					appendToken( q, discrim );
				}
			}
			else {
				Object constant;
				if (
						token.indexOf( '.' ) > -1 &&
						( constant = ReflectHelper.getConstantValue( token ) ) != null
				) {
					Type type;
					try {
						type = q.getFactory().getTypeResolver().heuristicType( constant.getClass().getName() );
					}
					catch ( MappingException me ) {
						throw new QueryException( me );
					}
					if ( type == null ) throw new QueryException( QueryTranslator.ERROR_CANNOT_DETERMINE_TYPE + token );
					try {
						appendToken( q, ( ( LiteralType ) type ).objectToSQLString( constant, q.getFactory().getDialect() ) );
					}
					catch ( Exception e ) {
						throw new QueryException( QueryTranslator.ERROR_CANNOT_FORMAT_LITERAL + token, e );
					}
				}
				else { //anything else

					String negatedToken = negated ? ( String ) NEGATIONS.get( token.toLowerCase() ) : null;
					if ( negatedToken != null && ( !betweenSpecialCase || !"or".equals( negatedToken ) ) ) {
						appendToken( q, negatedToken );
					}
					else {
						appendToken( q, token );
					}
				}
			}
		}
	}

	private void addToCurrentJoin(String sql) {
		( ( StringBuilder ) joins.getLast() ).append( sql );
	}

	private void addToCurrentJoin(PathExpressionParser.CollectionElement ce)
			throws QueryException {
		try {
			addToCurrentJoin( ce.joinSequence.toJoinFragment().toWhereFragmentString() + ce.indexValue.toString() );
		}
		catch ( MappingException me ) {
			throw new QueryException( me );
		}
	}

	private void specialCasesBefore(String lcToken) {
		if ( lcToken.equals( "between" ) || lcToken.equals( "not between" ) ) {
			betweenSpecialCase = true;
		}
	}

	private void specialCasesAfter(String lcToken) {
		if ( betweenSpecialCase && lcToken.equals( "and" ) ) {
			betweenSpecialCase = false;
		}
	}

	void appendToken(QueryTranslatorImpl q, String token) {
		if ( expectingIndex > 0 ) {
			pathExpressionParser.setLastCollectionElementIndexValue( token );
		}
		else {
			q.appendWhereToken( token );
		}
	}

	private boolean continuePathExpression(String token, QueryTranslatorImpl q) throws QueryException {

		expectingPathContinuation = false;

		PathExpressionParser.CollectionElement element = pathExpressionParser.lastCollectionElement();

		if ( token.startsWith( "." ) ) { // the path expression continues after a ]

			doPathExpression( getElementName( element, q ) + token, q ); // careful with this!

			addToCurrentJoin( element );
			return true; //NOTE: EARLY EXIT!

		}

		else { // the path expression ends at the ]
			if ( element.elementColumns.length != 1 ) {
				throw new QueryException( "path expression ended in composite collection element" );
			}
			appendToken( q, element.elementColumns[0] );
			addToCurrentJoin( element );
			return false;
		}
	}
}
