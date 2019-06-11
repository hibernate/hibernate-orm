/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.BitSet;

import org.hibernate.QueryException;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * The single available method {@link #parse} is responsible for parsing a
 * native query string and recognizing tokens defining named of ordinal
 * parameters and providing callbacks about each recognition.
 *
 * @author Steve Ebersole
 */
public class ParameterParser {
	private static final String HQL_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	private static final BitSet HQL_SEPARATORS_BITSET = new BitSet();

	static {
		for ( int i = 0; i < HQL_SEPARATORS.length(); i++ ) {
			HQL_SEPARATORS_BITSET.set( HQL_SEPARATORS.charAt( i ) );
		}
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
	public static void parse(String sqlString, ParameterRecognizer recognizer) throws QueryException {
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
					final int right = StringHelper.firstIndexOfChar( sqlString, HQL_SEPARATORS_BITSET, indx + 1 );
					final int chopLocation = right < 0 ? sqlString.length() : right;
					final String param = sqlString.substring( indx + 1, chopLocation );
					if ( param.isEmpty() ) {
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
						// a peek ahead showed this as a JPA-positional parameter
						final int right = StringHelper.firstIndexOfChar( sqlString, HQL_SEPARATORS, indx + 1 );
						final int chopLocation = right < 0 ? sqlString.length() : right;
						final String param = sqlString.substring( indx + 1, chopLocation );
						// make sure this "name" is an integral
						try {
							recognizer.jpaPositionalParameter( Integer.parseInt( param ), indx );
							indx = chopLocation - 1;
						}
						catch( NumberFormatException e ) {
							throw new QueryException( "JPA-style positional param was not an integral ordinal" );
						}
					}
					else {
						recognizer.ordinalParameter( indx );
					}
				}
				else {
					recognizer.other( c );
				}
			}
		}

		recognizer.complete();
	}

}
