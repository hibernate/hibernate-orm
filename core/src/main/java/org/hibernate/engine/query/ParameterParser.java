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
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class ParameterParser {

	public static interface Recognizer {
		public void outParameter(int position);
		public void ordinalParameter(int position);
		public void namedParameter(String name, int position);
		public void jpaPositionalParameter(String name, int position);
		public void other(char character);
	}

	private ParameterParser() {
		// disallow instantiation
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
	 * @throws QueryException
	 */
	public static void parse(String sqlString, Recognizer recognizer) throws QueryException {
		boolean hasMainOutputParameter = sqlString.indexOf( "call" ) > 0 &&
		                                 sqlString.indexOf( "?" ) < sqlString.indexOf( "call" ) &&
		                                 sqlString.indexOf( "=" ) < sqlString.indexOf( "call" );
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
						throw new QueryException("Space is not allowed after parameter prefix ':' '"
								+ sqlString + "'");
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

}
