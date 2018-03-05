/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.classic;
import java.util.Locale;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;

import static org.hibernate.hql.spi.QueryTranslator.ERROR_LEGACY_ORDINAL_PARAMS_NO_LONGER_SUPPORTED;

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
		else if ( token.startsWith( "?" ) ) {
			// ordinal query parameter
			if ( token.length() == 1 ) {
				throw new QueryException(
						String.format(
								Locale.ROOT,
								ERROR_LEGACY_ORDINAL_PARAMS_NO_LONGER_SUPPORTED,
								q.getQueryString()
						)
				);
			}
			else {
				final String labelString = token.substring( 1 );
				try {
					final int label = Integer.parseInt( labelString );
					q.addOrdinalParameter( label );
					q.appendOrderByToken( "?" );
				}
				catch (NumberFormatException e) {
					throw new QueryException( "Ordinal parameter label must be numeric : " + labelString, e );
				}
			}
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
