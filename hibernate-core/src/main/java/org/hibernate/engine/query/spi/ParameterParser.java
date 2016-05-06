/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.QueryException;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.util.StringHelper;

/**
 * The single available method {@link #parse} is responsible for parsing a
 * query string and recognizing tokens in relation to parameters (either
 * named, JPA-style, or ordinal) and providing callbacks about such
 * recognitions.
 *
 * @author Steve Ebersole
 */
public class ParameterParser {
	/**
	 * Maybe better named a Journaler.  Essentially provides a callback contract for things that recognize parameters
	 */
	public static interface Recognizer {
		/**
		 * Called when an output parameter is recognized
		 *
		 * @param position The position within the query
		 */
		public void outParameter(int position);

		/**
		 * Called when an ordinal parameter is recognized
		 *
		 * @param position The position within the query
		 */
		public void ordinalParameter(int position);

		/**
		 * Called when a named parameter is recognized
		 *
		 * @param name The recognized parameter name
		 * @param position The position within the query
		 */
		public void namedParameter(String name, int position);

		/**
		 * Called when a JPA-style named parameter is recognized
		 *
		 * @param name The name of the JPA-style parameter
		 * @param position The position within the query
		 */
		public void jpaPositionalParameter(String name, int position);

		/**
		 * Called when a character that is not a parameter (or part of a parameter dfinition) is recognized.
		 *
		 * @param character The recognized character
		 */
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
		final boolean hasMainOutputParameter = startsWithEscapeCallTemplate( sqlString );
		boolean foundMainOutputParam = false;

		final int stringLength = sqlString.length();

		boolean inSingleQuotes = false;
		boolean inDoubleQuotes = false;
		boolean inLineComment = false;
		boolean inDelimitedComment = false;

		for ( int indx = 0; indx < stringLength; indx++ ) {
			final char c = sqlString.charAt( indx );
			final boolean lastCharacter = indx == stringLength-1;

			// if we are "in" a certain context, check first for the end of that context
			if ( inSingleQuotes ) {
				recognizer.other( c );
				if ( '\'' == c ) {
					inSingleQuotes = false;
				}
			}
			else if ( inDoubleQuotes ) {
				recognizer.other( c );
				if ( '\"' == c ) {
					inDoubleQuotes = false;
				}
			}
			else if ( inDelimitedComment ) {
				recognizer.other( c );
				if ( !lastCharacter && '*' == c && '/' == sqlString.charAt( indx+1 ) ) {
					inDelimitedComment = false;
					recognizer.other( sqlString.charAt( indx+1 ) );
					indx++;
				}
			}
			else if ( inLineComment ) {
				recognizer.other( c );
				// see if the character ends the line
				if ( '\n' == c ) {
					inLineComment = false;
				}
				else if ( '\r' == c ) {
					inLineComment = false;
					if ( !lastCharacter && '\n' == sqlString.charAt( indx+1 ) ) {
						recognizer.other( sqlString.charAt( indx+1 ) );
						indx++;
					}
				}
			}
			// otherwise, see if we start such a context
			else if ( !lastCharacter && '/' == c && '*' == sqlString.charAt( indx+1 ) ) {
				inDelimitedComment = true;
				recognizer.other( c );
				recognizer.other( sqlString.charAt( indx+1 ) );
				indx++;
			}
			else if ( '-' == c ) {
				recognizer.other( c );
				if ( !lastCharacter && '-' == sqlString.charAt( indx+1 ) ) {
					inLineComment = true;
					recognizer.other( sqlString.charAt( indx+1 ) );
					indx++;
				}
			}
			else if ( '\"' == c ) {
				inDoubleQuotes = true;
				recognizer.other( c );
			}
			else if ( '\'' == c ) {
				inSingleQuotes = true;
				recognizer.other( c );
			}
			// special handling for backslash
			else if ( '\\' == c ) {
				// skip sending the backslash and instead send then next character, treating is as a literal
				recognizer.other( sqlString.charAt( ++indx ) );
			}
			// otherwise
			else {
				if ( c == ':' && indx < stringLength - 1 && sqlString.charAt( indx + 1 ) == ':') {
					// colon character has been escaped
					recognizer.other( c );
					indx++;
				}
				else if ( c == ':' ) {
					// named parameter
					final int right = StringHelper.firstIndexOfChar( sqlString, ParserHelper.HQL_SEPARATORS_BITSET, indx + 1 );
					final int chopLocation = right < 0 ? sqlString.length() : right;
					final String param = sqlString.substring( indx + 1, chopLocation );
					if ( StringHelper.isEmpty( param ) ) {
						throw new QueryException(
								"Space is not allowed afterQuery parameter prefix ':' [" + sqlString + "]"
						);
					}
					recognizer.namedParameter( param, indx );
					indx = chopLocation - 1;
				}
				else if ( c == '?' ) {
					// could be either an ordinal or JPA-positional parameter
					if ( indx < stringLength - 1 && Character.isDigit( sqlString.charAt( indx + 1 ) ) ) {
						// a peek ahead showed this as an JPA-positional parameter
						final int right = StringHelper.firstIndexOfChar( sqlString, ParserHelper.HQL_SEPARATORS, indx + 1 );
						final int chopLocation = right < 0 ? sqlString.length() : right;
						final String param = sqlString.substring( indx + 1, chopLocation );
						// make sure this "name" is an integral
						try {
							Integer.valueOf( param );
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

	/**
	 * Exposed as public solely for use from tests
	 *
	 * @param sqlString The SQL string to check
	 *
	 * @return true/false
	 */
	public static boolean startsWithEscapeCallTemplate(String sqlString) {
		if ( ! ( sqlString.startsWith( "{" ) && sqlString.endsWith( "}" ) ) ) {
			return false;
		}

		final int chopLocation = sqlString.indexOf( "call" );
		if ( chopLocation <= 0 ) {
			return false;
		}

		final String checkString = sqlString.substring( 1, chopLocation + 4 );
		final String fixture = "?=call";
		int fixturePosition = 0;
		boolean matches = true;
		final int max = checkString.length();
		for ( int i = 0; i < max; i++ ) {
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
