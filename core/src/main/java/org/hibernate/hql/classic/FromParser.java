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
package org.hibernate.hql.classic;

import org.hibernate.QueryException;
import org.hibernate.hql.QueryTranslator;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.JoinFragment;

import java.util.HashMap;
import java.util.Map;

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
	private int joinType;
	private boolean afterFetch;

	private static final int NONE = -666;

	private static final Map JOIN_TYPES = new HashMap();

	static {
		JOIN_TYPES.put( "left", new Integer( JoinFragment.LEFT_OUTER_JOIN ) );
		JOIN_TYPES.put( "right", new Integer( JoinFragment.RIGHT_OUTER_JOIN ) );
		JOIN_TYPES.put( "full", new Integer( JoinFragment.FULL_JOIN ) );
		JOIN_TYPES.put( "inner", new Integer( JoinFragment.INNER_JOIN ) );
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		// start by looking for HQL keywords...
		String lcToken = token.toLowerCase();
		if ( lcToken.equals( "," ) ) {
			if ( !( expectingJoin | expectingAs ) ) throw new QueryException( "unexpected token: ," );
			expectingJoin = false;
			expectingAs = false;
		}
		else if ( lcToken.equals( "join" ) ) {
			if ( !afterJoinType ) {
				if ( !( expectingJoin | expectingAs ) ) throw new QueryException( "unexpected token: join" );
				// inner joins can be abbreviated to 'join'
				joinType = JoinFragment.INNER_JOIN;
				expectingJoin = false;
				expectingAs = false;
			}
			else {
				afterJoinType = false;
			}
		}
		else if ( lcToken.equals( "fetch" ) ) {
			if ( q.isShallowQuery() ) throw new QueryException( QueryTranslator.ERROR_CANNOT_FETCH_WITH_ITERATE );
			if ( joinType == NONE ) throw new QueryException( "unexpected token: fetch" );
			if ( joinType == JoinFragment.FULL_JOIN || joinType == JoinFragment.RIGHT_OUTER_JOIN ) {
				throw new QueryException( "fetch may only be used with inner join or left outer join" );
			}
			afterFetch = true;
		}
		else if ( lcToken.equals( "outer" ) ) {
			// 'outer' is optional and is ignored
			if ( !afterJoinType ||
					( joinType != JoinFragment.LEFT_OUTER_JOIN && joinType != JoinFragment.RIGHT_OUTER_JOIN )
			) {
				throw new QueryException( "unexpected token: outer" );
			}
		}
		else if ( JOIN_TYPES.containsKey( lcToken ) ) {
			if ( !( expectingJoin | expectingAs ) ) throw new QueryException( "unexpected token: " + token );
			joinType = ( ( Integer ) JOIN_TYPES.get( lcToken ) ).intValue();
			afterJoinType = true;
			expectingJoin = false;
			expectingAs = false;
		}
		else if ( lcToken.equals( "class" ) ) {
			if ( !afterIn ) throw new QueryException( "unexpected token: class" );
			if ( joinType != NONE ) throw new QueryException( "outer or full join must be followed by path expression" );
			afterClass = true;
		}
		else if ( lcToken.equals( "in" ) ) {
			if ( !expectingIn ) throw new QueryException( "unexpected token: in" );
			afterIn = true;
			expectingIn = false;
		}
		else if ( lcToken.equals( "as" ) ) {
			if ( !expectingAs ) throw new QueryException( "unexpected token: as" );
			afterAs = true;
			expectingAs = false;
		}
		else {

			if ( afterJoinType ) throw new QueryException( "join expected: " + token );
			if ( expectingJoin ) throw new QueryException( "unexpected token: " + token );
			if ( expectingIn ) throw new QueryException( "in expected: " + token );

			// now anything that is not a HQL keyword

			if ( afterAs || expectingAs ) {

				// (AS is always optional, for consistency with SQL/OQL)

				// process the "new" HQL style where aliases are assigned
				// _after_ the class name or path expression ie. using
				// the AS construction

				if ( entityName != null ) {
					q.setAliasName( token, entityName );
				}
				else {
					throw new QueryException( "unexpected: as " + token );
				}
				afterAs = false;
				expectingJoin = true;
				expectingAs = false;
				entityName = null;

			}
			else if ( afterIn ) {

				// process the "old" HQL style where aliases appear _first_
				// ie. using the IN or IN CLASS constructions

				if ( alias == null ) throw new QueryException( "alias not specified for: " + token );

				if ( joinType != NONE ) throw new QueryException( "outer or full join must be followed by path expression" );

				if ( afterClass ) {
					// treat it as a classname
					Queryable p = q.getEntityPersisterUsingImports( token );
					if ( p == null ) throw new QueryException( "persister not found: " + token );
					q.addFromClass( alias, p );
				}
				else {
					// treat it as a path expression
					peParser.setJoinType( JoinFragment.INNER_JOIN );
					peParser.setUseThetaStyleJoin( true );
					ParserHelper.parse( peParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
					if ( !peParser.isCollectionValued() ) throw new QueryException( "path expression did not resolve to collection: " + token );
					String nm = peParser.addFromCollection( q );
					q.setAliasName( alias, nm );
				}

				alias = null;
				afterIn = false;
				afterClass = false;
				expectingJoin = true;
			}
			else {

				// handle a path expression or class name that
				// appears at the start, in the "new" HQL
				// style or an alias that appears at the start
				// in the "old" HQL style

				Queryable p = q.getEntityPersisterUsingImports( token );
				if ( p != null ) {
					// starts with the name of a mapped class (new style)
					if ( joinType != NONE ) throw new QueryException( "outer or full join must be followed by path expression" );
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
					if ( joinType != NONE ) {
						peParser.setJoinType( joinType );
					}
					else {
						peParser.setJoinType( JoinFragment.INNER_JOIN );
					}
					peParser.setUseThetaStyleJoin( q.isSubquery() );

					ParserHelper.parse( peParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
					entityName = peParser.addFromAssociation( q );

					joinType = NONE;
					peParser.setJoinType( JoinFragment.INNER_JOIN );

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
		alias = null;
		afterIn = false;
		afterAs = false;
		afterClass = false;
		expectingJoin = false;
		expectingIn = false;
		expectingAs = false;
		joinType = NONE;
	}

	public void end(QueryTranslatorImpl q) {
	}

}
