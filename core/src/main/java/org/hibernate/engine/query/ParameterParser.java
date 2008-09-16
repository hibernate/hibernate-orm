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
package org.hibernate.engine.query;

import org.hibernate.QueryException;
import org.hibernate.hql.classic.ParserHelper;
import org.hibernate.util.StringHelper;

/**
 * The single available method {@link #parse} is responsible for parsing a
 * query string and recognizing tokens in relation to parameters (either
 * named, JPA-style, or ordinal) and providing callbacks about such
 * recognitions.
 *
 * @author Steve Ebersole
 */
public class ParameterParser {

	public static interface Recognizer {
		public void outParameter(int position);
		public void ordinalParameter(int position);
		public void namedParameter(String name, int position);
		public void jpaPositionalParameter(String name, int position);
		public void other(char character);
	}

	/**
	 * Direct instantiation of ParameterParser disallowed.
	 */
	private ParameterParser() {
	}

	/**
	 * Performs the actual parsing and tokenizing of the query string making appropriate
	 * callbacks to the given recognizer upon recognition of the various tokens.
	 * <p/>
	 * Note that currently, this only knows how to deal with a single output
	 * parameter (for callable statements).  If we later add support for
	 * multiple output params, this, obviously, needs to change.
	 *
	 * @param sqlString The string to be parsed/tokenized.
	 * @param recognizer The thing which handles recognition events.
	 * @throws QueryException Indicates unexpected parameter conditions.
	 */
	public static void parse(String sqlString, Recognizer recognizer) throws QueryException {
		boolean hasMainOutputParameter = startsWithEscapeCallTemplate( sqlString );
		boolean foundMainOutputParam = false;

		int stringLength = sqlString.length();
		boolean inQuote = false;
		for ( int indx = 0; indx < stringLength; indx++ ) {
			char c = sqlString.charAt( indx );
			if ( inQuote ) {
				if ( '\'' == c ) {
					inQuote = false;
				}
				recognizer.other( c );
			}
			else if ( '\'' == c ) {
				inQuote = true;
				recognizer.other( c );
			}
			else {
				if ( c == ':' ) {
					// named parameter
					int right = StringHelper.firstIndexOfChar( sqlString, ParserHelper.HQL_SEPARATORS, indx + 1 );
					int chopLocation = right < 0 ? sqlString.length() : right;
					String param = sqlString.substring( indx + 1, chopLocation );
					if ( StringHelper.isEmpty( param ) ) {
						throw new QueryException(
								"Space is not allowed after parameter prefix ':' [" + sqlString + "]"
						);
					}
					recognizer.namedParameter( param, indx );
					indx = chopLocation - 1;
				}
				else if ( c == '?' ) {
					// could be either an ordinal or JPA-positional parameter
					if ( indx < stringLength - 1 && Character.isDigit( sqlString.charAt( indx + 1 ) ) ) {
						// a peek ahead showed this as an JPA-positional parameter
						int right = StringHelper.firstIndexOfChar( sqlString, ParserHelper.HQL_SEPARATORS, indx + 1 );
						int chopLocation = right < 0 ? sqlString.length() : right;
						String param = sqlString.substring( indx + 1, chopLocation );
						// make sure this "name" is an integral
						try {
							new Integer( param );
						}
						catch( NumberFormatException e ) {
							throw new QueryException( "JPA-style positional param was not an integral ordinal" );
						}
						recognizer.jpaPositionalParameter( param, indx );
						indx = chopLocation - 1;
					}
					else {
						if ( hasMainOutputParameter && !foundMainOutputParam ) {
							foundMainOutputParam = true;
							recognizer.outParameter( indx );
						}
						else {
							recognizer.ordinalParameter( indx );
						}
					}
				}
				else {
					recognizer.other( c );
				}
			}
		}
	}

	public static boolean startsWithEscapeCallTemplate(String sqlString) {
		if ( ! ( sqlString.startsWith( "{" ) && sqlString.endsWith( "}" ) ) ) {
			return false;
		}

		int chopLocation = sqlString.indexOf( "call" );
		if ( chopLocation <= 0 ) {
			return false;
		}

		final String checkString = sqlString.substring( 1, chopLocation + 4 );
		final String fixture = "?=call";
		int fixturePosition = 0;
		boolean matches = true;
		for ( int i = 0, max = checkString.length(); i < max; i++ ) {
			final char c = Character.toLowerCase( checkString.charAt( i ) );
			if ( Character.isWhitespace( c ) ) {
				continue;
			}
			if ( c == fixture.charAt( fixturePosition ) ) {
				fixturePosition++;
				continue;
			}
			matches = false;
			break;
		}

		return matches;
	}

}
