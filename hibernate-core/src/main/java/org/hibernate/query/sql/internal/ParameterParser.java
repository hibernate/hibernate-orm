/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.BitSet;
import java.util.Map;
import org.hibernate.QueryException;
import org.hibernate.QueryParameterException;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.query.ParameterLabelException;
import org.hibernate.query.sql.spi.ParameterRecognizer;

/**
 * The single available method {@link #parse} is responsible for parsing a
 * native query string and recognizing tokens defining named of ordinal
 * parameters and providing callbacks about each recognition.
 *
 * @author Steve Ebersole
 */
public class ParameterParser {
	private static final String HQL_SEPARATORS = " \n\r\f\t,;()=<>&|+-=/*'^![]#~\\";
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
	 * <p>
	 * Note that currently, this only knows how to deal with a single output
	 * parameter (for callable statements).  If we later add support for
	 * multiple output params, this, obviously, needs to change.
	 *
	 * @param sqlString The string to be parsed/tokenized.
	 * @param recognizer The thing which handles recognition events.
	 * @param nativeJdbcParametersIgnored Whether to ignore ordinal parameters in native queries or not.
	 * @throws QueryException Indicates unexpected parameter conditions.
	 */
	public static void parse(String sqlString, ParameterRecognizer recognizer, boolean nativeJdbcParametersIgnored) throws QueryException {
		checkIsNotAFunctionCall( sqlString );
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
				if ( c == ':' ) {
					if ( indx < stringLength - 1 && Character.isJavaIdentifierStart( sqlString.charAt( indx + 1 ) ) ) {
						// named parameter
						final int right = StringHelper.firstIndexOfChar( sqlString, HQL_SEPARATORS_BITSET, indx + 1 );
						final int chopLocation = right < 0 ? sqlString.length() : right;
						final String param = sqlString.substring( indx + 1, chopLocation );
						if ( param.isEmpty() ) {
							throw new QueryParameterException(
									"Space is not allowed after parameter prefix ':'",
									sqlString
							);
						}
						recognizer.namedParameter( param, indx );
						indx = chopLocation - 1;
					}
					else {
						// For backwards compatibility, allow some known operators in the escaped form
						if ( indx < stringLength - 3
								&& sqlString.charAt( indx + 1 ) == ':'
								&& sqlString.charAt( indx + 2 ) == ':'
								&& sqlString.charAt( indx + 3 ) == ':' ) {
							// Detect the :: operator, escaped as ::::
							DeprecationLogger.DEPRECATION_LOGGER.deprecatedNativeQueryColonEscaping( "::::", "::" );
							recognizer.other( ':' );
							recognizer.other( ':' );
							indx += 3;
						}
						else if ( indx < stringLength - 2
								&& sqlString.charAt( indx + 1 ) == ':'
								&& sqlString.charAt( indx + 2 ) == '=' ) {
							// Detect the := operator, escaped as ::=
							DeprecationLogger.DEPRECATION_LOGGER.deprecatedNativeQueryColonEscaping( "::=", ":=" );
							recognizer.other( ':' );
							recognizer.other( '=' );
							indx += 2;
						}
						else {
							recognizer.other( ':' );
							// Consume all following colons as they are eagerly to not confuse named parameter detection
							while ( indx < stringLength - 1
									&& sqlString.charAt( indx + 1 ) == ':' ) {
								indx++;
								recognizer.other( ':' );
							}
						}
					}
				}
				else if ( c == '?' ) {
					// could be either a positional or JPA-style ordinal parameter
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
							throw new ParameterLabelException( "Ordinal parameter label was not an integer" );
						}
					}
					else {
						if ( !nativeJdbcParametersIgnored ) {
							recognizer.ordinalParameter( indx );
						}
					}
				}
				else {
					recognizer.other( c );
				}
			}
		}

		recognizer.complete();
	}

	public static void parse(String sqlString, ParameterRecognizer recognizer) throws QueryException {
		parse( sqlString, recognizer, false );
	}

	private static void checkIsNotAFunctionCall(String sqlString) {
		final String trimmed = sqlString.trim();
		if ( !( trimmed.startsWith( "{" ) && trimmed.endsWith( "}" ) ) ) {
			return;
		}

		final int chopLocation = trimmed.indexOf( "call" );
		if ( chopLocation <= 0 ) {
			return;
		}

		final String checkString = trimmed.substring( 1, chopLocation + 4 );
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

		if ( matches ) {
			throw new UnsupportedOperationException(
					"Recognizing native query as a function call is no longer supported" );

		}
	}

}
