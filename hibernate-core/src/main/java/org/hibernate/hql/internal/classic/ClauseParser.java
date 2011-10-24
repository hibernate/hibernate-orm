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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.QueryException;

/**
 * Parses the Hibernate query into its constituent clauses.
 */
public class ClauseParser implements Parser {

	private Parser child;
	private List selectTokens;
	private boolean cacheSelectTokens = false;
	private boolean byExpected = false;
	private int parenCount = 0;

	public void token(String token, QueryTranslatorImpl q) throws QueryException {
		String lcToken = token.toLowerCase();

		if ( "(".equals( token ) ) {
			parenCount++;
		}
		else if ( ")".equals( token ) ) {
			parenCount--;
		}

		if ( byExpected && !lcToken.equals( "by" ) ) {
			throw new QueryException( "BY expected after GROUP or ORDER: " + token );
		}

		boolean isClauseStart = parenCount == 0; //ignore subselect keywords

		if ( isClauseStart ) {
			if ( lcToken.equals( "select" ) ) {
				selectTokens = new ArrayList();
				cacheSelectTokens = true;
			}
			else if ( lcToken.equals( "from" ) ) {
				child = new FromParser();
				child.start( q );
				cacheSelectTokens = false;
			}
			else if ( lcToken.equals( "where" ) ) {
				endChild( q );
				child = new WhereParser();
				child.start( q );
			}
			else if ( lcToken.equals( "order" ) ) {
				endChild( q );
				child = new OrderByParser();
				byExpected = true;
			}
			else if ( lcToken.equals( "having" ) ) {
				endChild( q );
				child = new HavingParser();
				child.start( q );
			}
			else if ( lcToken.equals( "group" ) ) {
				endChild( q );
				child = new GroupByParser();
				byExpected = true;
			}
			else if ( lcToken.equals( "by" ) ) {
				if ( !byExpected ) throw new QueryException( "GROUP or ORDER expected before BY" );
				child.start( q );
				byExpected = false;
			}
			else {
				isClauseStart = false;
			}
		}

		if ( !isClauseStart ) {
			if ( cacheSelectTokens ) {
				selectTokens.add( token );
			}
			else {
				if ( child == null ) {
					throw new QueryException( "query must begin with SELECT or FROM: " + token );
				}
				else {
					child.token( token, q );
				}
			}
		}

	}

	private void endChild(QueryTranslatorImpl q) throws QueryException {
		if ( child == null ) {
			//null child could occur for no from clause in a filter
			cacheSelectTokens = false;
		}
		else {
			child.end( q );
		}
	}

	public void start(QueryTranslatorImpl q) {
	}

	public void end(QueryTranslatorImpl q) throws QueryException {
		endChild( q );
		if ( selectTokens != null ) {
			child = new SelectParser();
			child.start( q );
			Iterator iter = selectTokens.iterator();
			while ( iter.hasNext() ) {
				token( ( String ) iter.next(), q );
			}
			child.end( q );
		}
		byExpected = false;
		parenCount = 0;
		cacheSelectTokens = false;
	}

}







