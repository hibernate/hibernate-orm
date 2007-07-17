//$Id: ClauseParser.java 4907 2004-12-08 00:24:14Z oneovthafew $
package org.hibernate.hql.classic;

import org.hibernate.QueryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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







