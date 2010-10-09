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
