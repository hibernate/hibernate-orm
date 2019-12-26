/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.QueryException;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinType;

/**
 * Parses the from clause of a hibernate query, looking for tables and
 * aliases for the SQL query.
 */

public class FromParser implements Parser {

	private final PathExpressionParser peParser = new FromPathExpressionParser();
	private String entityName;
	private String alias;
	private boolean afterIn;
	private boolean afterAs;
	private boolean afterClass;
	private boolean expectingJoin;
	private boolean expectingIn;
	private boolean expectingAs;
	private boolean afterJoinType;
	private JoinType joinType = JoinType.INNER_JOIN;
	private boolean afterFetch;

	//support collection member declarations
	//e.g. "from Customer c, in(c.orders) as o"
	private boolean memberDeclarations;
	private boolean expectingPathExpression;
	private boolean afterMemberDeclarations;
	private String collectionName;

	private static final Map<String, JoinType> JOIN_TYPES = new HashMap<String, JoinType>();

	static {
		JOIN_TYPES.put( "left", JoinType.LEFT_OUTER_JOIN );
		JOIN_TYPES.put( "right", JoinType.RIGHT_OUTER_JOIN );
		JOIN_TYPES.put( "full", JoinType.FULL_JOIN );
		JOIN_TYPES.put( "inner", JoinType.INNER_JOIN );
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		// start by looking for HQL keywords...
		String lcToken = token.toLowerCase( Locale.ROOT );
		if ( lcToken.equals( "," ) ) {
			if ( !( expectingJoin | expectingAs ) ) {
				throw new QueryException( "unexpected token: ," );
			}
			expectingJoin = false;
			expectingAs = false;
		}
		else if ( lcToken.equals( "join" ) ) {
			if ( !afterJoinType ) {
				if ( !( expectingJoin | expectingAs ) ) {
					throw new QueryException( "unexpected token: join" );
				}
				// inner joins can be abbreviated to 'join'
				joinType = JoinType.INNER_JOIN;
				expectingJoin = false;
				expectingAs = false;
			}
			else {
				afterJoinType = false;
			}
		}
		else if ( lcToken.equals( "fetch" ) ) {
			if ( q.isShallowQuery() ) {
				throw new QueryException( QueryTranslator.ERROR_CANNOT_FETCH_WITH_ITERATE );
			}
			if ( joinType == JoinType.NONE ) {
				throw new QueryException( "unexpected token: fetch" );
			}
			if ( joinType == JoinType.FULL_JOIN || joinType == JoinType.RIGHT_OUTER_JOIN ) {
				throw new QueryException( "fetch may only be used with inner join or left outer join" );
			}
			afterFetch = true;
		}
		else if ( lcToken.equals( "outer" ) ) {
			// 'outer' is optional and is ignored
			if ( !afterJoinType ||
					( joinType != JoinType.LEFT_OUTER_JOIN && joinType != JoinType.RIGHT_OUTER_JOIN )
					) {
				throw new QueryException( "unexpected token: outer" );
			}
		}
		else if ( JOIN_TYPES.containsKey( lcToken ) ) {
			if ( !( expectingJoin | expectingAs ) ) {
				throw new QueryException( "unexpected token: " + token );
			}
			joinType = JOIN_TYPES.get( lcToken );
			afterJoinType = true;
			expectingJoin = false;
			expectingAs = false;
		}
		else if ( lcToken.equals( "class" ) ) {
			if ( !afterIn ) {
				throw new QueryException( "unexpected token: class" );
			}
			if ( joinType != JoinType.NONE ) {
				throw new QueryException( "outer or full join must be followed by path expression" );
			}
			afterClass = true;
		}
		else if ( lcToken.equals( "in" ) ) {
			if ( alias == null ) {
				memberDeclarations = true;
				afterMemberDeclarations = false;
			}
			else if ( !expectingIn ) {
				throw new QueryException( "unexpected token: in" );
			}
			else {
				afterIn = true;
				expectingIn = false;
			}
		}
		else if ( lcToken.equals( "as" ) ) {
			if ( !expectingAs ) {
				throw new QueryException( "unexpected token: as" );
			}
			afterAs = true;
			expectingAs = false;
		}
		else if ( "(".equals( token ) ) {
			if ( !memberDeclarations ) {
				throw new QueryException( "unexpected token: (" );
			}
			//TODO alias should be null here
			expectingPathExpression = true;

		}
		else if ( ")".equals( token ) ) {
//			memberDeclarations = false;
//			expectingPathExpression = false;
			afterMemberDeclarations = true;
		}
		else {
			if ( afterJoinType ) {
				throw new QueryException( "join expected: " + token );
			}
			if ( expectingJoin ) {
				throw new QueryException( "unexpected token: " + token );
			}
			if ( expectingIn ) {
				throw new QueryException( "in expected: " + token );
			}

			// now anything that is not a HQL keyword

			if ( afterAs || expectingAs ) {

				// (AS is always optional, for consistency with SQL/OQL)

				// process the "new" HQL style where aliases are assigned
				// _after_ the class name or path expression ie. using
				// the AS construction

				if ( entityName != null ) {
					q.setAliasName( token, entityName );
				}
				else if ( collectionName != null ) {
					q.setAliasName( token, collectionName );
				}
				else {
					throw new QueryException( "unexpected: as " + token );
				}
				afterAs = false;
				expectingJoin = true;
				expectingAs = false;
				entityName = null;
				collectionName = null;
				memberDeclarations = false;
				expectingPathExpression = false;
				afterMemberDeclarations = false;

			}
			else if ( afterIn ) {

				// process the "old" HQL style where aliases appear _first_
				// ie. using the IN or IN CLASS constructions

				if ( alias == null ) {
					throw new QueryException( "alias not specified for: " + token );
				}

				if ( joinType != JoinType.NONE ) {
					throw new QueryException( "outer or full join must be followed by path expression" );
				}

				if ( afterClass ) {
					// treat it as a classname
					Queryable p = q.getEntityPersisterUsingImports( token );
					if ( p == null ) {
						throw new QueryException( "persister not found: " + token );
					}
					q.addFromClass( alias, p );
				}
				else {
					// treat it as a path expression
					peParser.setJoinType( JoinType.INNER_JOIN );
					peParser.setUseThetaStyleJoin( true );
					ParserHelper.parse( peParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
					if ( !peParser.isCollectionValued() ) {
						throw new QueryException(
								"path expression did not resolve to collection: " + token
						);
					}
					String nm = peParser.addFromCollection( q );
					q.setAliasName( alias, nm );
				}

				alias = null;
				afterIn = false;
				afterClass = false;
				expectingJoin = true;
			}
			else if ( memberDeclarations && expectingPathExpression ) {
				expectingAs = true;
				peParser.setJoinType( JoinType.INNER_JOIN );
				peParser.setUseThetaStyleJoin( false );
				ParserHelper.parse( peParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
				if ( !peParser.isCollectionValued() ) {
					throw new QueryException( "path expression did not resolve to collection: " + token );
				}
				collectionName = peParser.addFromCollection( q );
				expectingPathExpression = false;
				memberDeclarations = false;
			}
			else {

				// handle a path expression or class name that
				// appears at the start, in the "new" HQL
				// style or an alias that appears at the start
				// in the "old" HQL style

				Queryable p = q.getEntityPersisterUsingImports( token );
				if ( p != null ) {
					// starts with the name of a mapped class (new style)
					if ( joinType != JoinType.NONE ) {
						throw new QueryException( "outer or full join must be followed by path expression" );
					}
					entityName = q.createNameFor( p.getEntityName() );
					q.addFromClass( entityName, p );
					expectingAs = true;
				}
				else if ( token.indexOf( '.' ) < 0 ) {
					// starts with an alias (old style)
					// semi-bad thing about this: can't re-alias another alias.....
					alias = token;
					expectingIn = true;
				}
				else {

					// starts with a path expression (new style)

					// force HQL style: from Person p inner join p.cars c
					//if (joinType==NONE) throw new QueryException("path expression must be preceded by full, left, right or inner join");

					//allow ODMG OQL style: from Person p, p.cars c
					if ( joinType != JoinType.NONE ) {
						peParser.setJoinType( joinType );
					}
					else {
						peParser.setJoinType( JoinType.INNER_JOIN );
					}
					peParser.setUseThetaStyleJoin( q.isSubquery() );

					ParserHelper.parse( peParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
					entityName = peParser.addFromAssociation( q );

					joinType = JoinType.NONE;
					peParser.setJoinType( JoinType.INNER_JOIN );

					if ( afterFetch ) {
						peParser.fetch( q, entityName );
						afterFetch = false;
					}

					expectingAs = true;

				}
			}
		}

	}

	public void start(QueryTranslatorImpl q) {
		entityName = null;
		collectionName = null;
		alias = null;
		afterIn = false;
		afterAs = false;
		afterClass = false;
		expectingJoin = false;
		expectingIn = false;
		expectingAs = false;
		memberDeclarations = false;
		expectingPathExpression = false;
		afterMemberDeclarations = false;
		joinType = JoinType.NONE;
	}

	public void end(QueryTranslatorImpl q) {
		if ( afterMemberDeclarations ) {
			//The exception throwned by the AST query translator contains the error token location, represented by line and column,
			//but it hard to get that info here.
			throw new QueryException( "alias not specified for IN" );
		}
	}

}
