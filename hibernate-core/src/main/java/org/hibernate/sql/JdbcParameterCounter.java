/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;

import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isLetter;
import static java.lang.Character.isLetterOrDigit;
import static java.lang.Character.isWhitespace;

/**
 * Counts JDBC parameter markers in custom SQL without interpreting the statement.
 */
@Internal
public final class JdbcParameterCounter {
	private JdbcParameterCounter() {
	}

	public static int count(String sql, Dialect dialect) {
		final boolean mysql = dialect instanceof MySQLDialect;
		final boolean backslashEscapes =
				dialect instanceof MySQLDialect mySqlDialect
					&& !mySqlDialect.isNoBackslashEscapesEnabled();
		final boolean escapedQuestionMarks =
				dialect instanceof PostgreSQLDialect
				|| dialect instanceof CockroachDialect;
		final var identifiers = dialect.getIdentifierSupport();
		int count = 0;
		for ( int i = 0; i < sql.length(); i++ ) {
			final char c = sql.charAt( i );
			final char next = i + 1 < sql.length() ? sql.charAt( i + 1 ) : 0;
			if ( c == '-' && next == '-'
					&& ( !mysql || i + 2 == sql.length() || isWhitespace( sql.charAt( i + 2 ) ) )
					|| mysql && c == '#' ) {
				while ( i + 1 < sql.length() && sql.charAt( i + 1 ) != '\n' && sql.charAt( i + 1 ) != '\r' ) {
					i++;
				}
			}
			else if ( c == '/' && next == '*' ) {
				i = skipComment( sql, i );
			}
			else if ( ( c == 'q' || c == 'Q' ) && next == '\'' && tokenStart( sql, i ) && i + 2 < sql.length() ) {
				final char close = switch ( sql.charAt( i + 2 ) ) {
					case '[' -> ']';
					case '(' -> ')';
					case '{' -> '}';
					case '<' -> '>';
					default -> sql.charAt( i + 2 );
				};
				i = closingDelimiter( sql, "" + close + '\'', i + 3 );
			}
			else if ( c == '\'' || c == '"' || c == identifiers.openQuote() ) {
				final char close = c == identifiers.openQuote() ? identifiers.closeQuote() : c;
				final boolean escapeString = c == '\'' && i > 0
						&& ( sql.charAt( i - 1 ) == 'e' || sql.charAt( i - 1 ) == 'E' ) && tokenStart( sql, i - 1 );
				i = skipQuoted( sql, i, close, escapeString || backslashEscapes && ( c == '\'' || c == '"' ) );
			}
			else if ( c == '$' && tokenStart( sql, i ) ) {
				int end = i + 1;
				if ( end < sql.length() && ( sql.charAt( end ) == '_' || isLetter( sql.charAt( end ) ) ) ) {
					while ( end < sql.length() && ( sql.charAt( end ) == '_' || isLetterOrDigit( sql.charAt( end ) ) ) ) {
						end++;
					}
				}
				if ( end < sql.length() && sql.charAt( end ) == '$' ) {
					i = closingDelimiter( sql, sql.substring( i, end + 1 ), end + 1 );
				}
			}
			else if ( c == '?' ) {
				if ( escapedQuestionMarks && next == '?' ) {
					i++;
				}
				else {
					count++;
				}
			}
		}
		return count;
	}

	private static boolean tokenStart(String sql, int offset) {
		return offset == 0 || !isJavaIdentifierPart( sql.charAt( offset - 1 ) );
	}

	private static int closingDelimiter(String sql, String delimiter, int start) {
		final int end = sql.indexOf( delimiter, start );
		if ( end < 0 ) {
			throw new MappingException( "Unterminated quoted text in custom SQL: " + sql );
		}
		return end + delimiter.length() - 1;
	}

	private static int skipQuoted(String sql, int start, char close, boolean backslashEscapes) {
		for ( int i = start + 1; i < sql.length(); i++ ) {
			if ( backslashEscapes && sql.charAt( i ) == '\\' ) {
				i++;
			}
			else if ( sql.charAt( i ) == close ) {
				if ( i + 1 < sql.length() && sql.charAt( i + 1 ) == close ) {
					i++;
				}
				else {
					return i;
				}
			}
		}
		throw new MappingException( "Unterminated quoted text in custom SQL: " + sql );
	}

	private static int skipComment(String sql, int start) {
		int depth = 1;
		for ( int i = start + 2; i + 1 < sql.length(); i++ ) {
			if ( sql.charAt( i ) == '/' && sql.charAt( i + 1 ) == '*' ) {
				depth++;
				i++;
			}
			else if ( sql.charAt( i ) == '*' && sql.charAt( i + 1 ) == '/' ) {
				i++;
				if ( --depth == 0 ) {
					return i;
				}
			}
		}
		throw new MappingException( "Unterminated comment in custom SQL: " + sql );
	}
}
