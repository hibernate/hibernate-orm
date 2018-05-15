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
import org.hibernate.query.spi.ParameterRecognizer;

import org.jboss.logging.Logger;

/**
 * Coordinates "parsing" a native query's SQL string looking for parameter markers
 * and calling out to the given Recognizer.
 *
 * The single available method {@link #parse} is responsible for parsing a
 * query string and recognizing tokens in relation to parameters (either
 * named, JPA-style, or ordinal) and providing callbacks about such
 * recognitions.
 *
 * @author Steve Ebersole
 */
public class ParameterParser {
	private static final Logger log = Logger.getLogger( ParameterParser.class );

	public static final String HQL_SEPARATORS = " \n\r\f\t,()=<>&|+-=/*'^![]#~\\";
	public static final BitSet HQL_SEPARATORS_BITSET = new BitSet();

	static {
		for ( int i = 0; i < HQL_SEPARATORS.length(); i++ ) {
			HQL_SEPARATORS_BITSET.set( HQL_SEPARATORS.charAt( i ) );
		}
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
		final boolean hasMainOutputParameter = determineQueryType( sqlString ) == Type.CALL_FUNCTION;
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
					final int right = StringHelper.firstIndexOfChar( sqlString, HQL_SEPARATORS_BITSET, indx + 1 );
					final int chopLocation = right < 0 ? sqlString.length() : right;
					final String param = sqlString.substring( indx + 1, chopLocation );
					if ( StringHelper.isEmpty( param ) ) {
						throw new QueryException(
								"Space is not allowed after parameter prefix ':' [" + sqlString + "]"
						);
					}
					recognizer.namedParameter( param, indx );
					indx = chopLocation - 1;
				}
				else if ( c == '?' ) {
					// could be either a positional of a JDBC-style ordinal parameter
					if ( indx < stringLength - 1 && Character.isDigit( sqlString.charAt( indx + 1 ) ) ) {
						// a peek ahead showed this as an JPA-positional parameter
						final int right = StringHelper.firstIndexOfChar( sqlString, HQL_SEPARATORS, indx + 1 );
						final int chopLocation = right < 0 ? sqlString.length() : right;
						final String param = sqlString.substring( indx + 1, chopLocation );
						// make sure this "name" is an integer value
						try {
							recognizer.jpaPositionalParameter( Integer.valueOf( param ), indx );
							indx = chopLocation - 1;
						}
						catch( NumberFormatException e ) {
							throw new QueryException( "JPA-style positional param was not an integer value : " + param );
						}
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

	enum Type {
		/**
		 * Indicates a query string not using JDBC's "call escape" syntax.  This
		 * could be a SELECT, DELETE, etc query.
		 */
		NORMAL,
		/**
		 * Indicates a query string not using JDBC's procedure "call escape" syntax, i.e.:
		 * {@code "{call...}"}.
		 */
		CALL_PROCEDURE,
		/**
		 * Indicates a query string not using JDBC's function "call escape" syntax, i.e.:
		 * {@code "{?=call...}"}.
		 */
		CALL_FUNCTION
	}

	/**
	 * Exposed as public solely for use from tests
	 *
	 * @param sql The SQL string to check
	 *
	 * @return true/false
	 */
	public static Type determineQueryType(String sql) {
		sql = sql.trim();

		if ( ! ( sql.startsWith( "{" ) && sql.endsWith( "}" ) ) ) {
			return Type.NORMAL;
		}

		final String trimmed = StringHelper.stripBookends( sql, 1 ).trim();
		final int callStartPosition = trimmed.indexOf( "call" );
		if ( callStartPosition <= 0 ) {
			log.debugf( "SQL query is wrapped in braces, but contained no `call` keyword; returning Type.NORMAL but that seems unintended : %s", sql );
			return Type.NORMAL;
		}

		// so we know we have a JDBC call escape syntax, need to find out if it
		// is the procedure or function variant...
		if ( callStartPosition == 1 ) {
			// `call` is the first piece inside the escape syntax, cannot be a function call variant
			return Type.CALL_PROCEDURE;
		}

		final int firstParamPosition = trimmed.indexOf( "?" );
		if ( firstParamPosition < callStartPosition ) {
			return Type.CALL_FUNCTION;
		}
		else {
			return Type.CALL_PROCEDURE;
		}
	}

	/**
	 * Exposed as public solely for use from tests
	 *
	 * @param sqlString The SQL string to check
	 *
	 * @return true/false
	 *
	 * @deprecated (since 6.0) use {@link #determineQueryType(String)} instead
	 */
	@Deprecated
	public static boolean startsWithEscapeCallTemplate(String sqlString) {
		// the result of this historically was true only if the sqlString was using
		// function call syntax specifically
		return determineQueryType( sqlString ) == Type.CALL_FUNCTION;
	}

	/**
	 * Direct instantiation of ParameterParser disallowed.
	 */
	private ParameterParser() {
	}
}
