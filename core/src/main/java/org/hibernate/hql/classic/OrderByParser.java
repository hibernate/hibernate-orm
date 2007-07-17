//$Id: OrderByParser.java 4907 2004-12-08 00:24:14Z oneovthafew $
package org.hibernate.hql.classic;

import org.hibernate.QueryException;
import org.hibernate.util.StringHelper;

/**
 * Parses the ORDER BY clause of a query
 */

public class OrderByParser implements Parser {

	// This uses a PathExpressionParser but notice that compound paths are not valid,
	// only bare names and simple paths:

	// SELECT p FROM p IN CLASS eg.Person ORDER BY p.Name, p.Address, p

	// The reason for this is SQL doesn't let you sort by an expression you are
	// not returning in the result set.

	private final PathExpressionParser pathExpressionParser;

	{
		pathExpressionParser = new PathExpressionParser();
		pathExpressionParser.setUseThetaStyleJoin( true ); //TODO: would be nice to use false, but issues with MS SQL
	}

	public void token(String token, QueryTranslatorImpl q) throws QueryException {

		if ( q.isName( StringHelper.root( token ) ) ) {
			ParserHelper.parse( pathExpressionParser, q.unalias( token ), ParserHelper.PATH_SEPARATORS, q );
			q.appendOrderByToken( pathExpressionParser.getWhereColumn() );
			pathExpressionParser.addAssociation( q );
		}
		else if ( token.startsWith( ParserHelper.HQL_VARIABLE_PREFIX ) ) { //named query parameter
			q.addNamedParameter( token.substring( 1 ) );
			q.appendOrderByToken( "?" );
		}
		else {
			q.appendOrderByToken( token );
		}
	}

	public void start(QueryTranslatorImpl q) throws QueryException {
	}

	public void end(QueryTranslatorImpl q) throws QueryException {
	}

}
