/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Performs formatting of DDL SQL statements.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class DDLFormatterImpl implements Formatter {

	private static final String INITIAL_LINE = System.lineSeparator() + "    ";
	private static final String OTHER_LINES = System.lineSeparator() + "       ";
	/**
	 * Singleton access
	 */
	public static final DDLFormatterImpl INSTANCE = new DDLFormatterImpl();

	private static final Set<String> BREAKS = Set.of( "drop", "alter", "modify", "add", "references", "foreign", "on" );
	private static final Set<String> QUOTES = Set.of( "\"", "`", "]", "[", "'" );

	@Override
	public String format(String sql) {
		if ( isEmpty( sql ) ) {
			return sql;
		}

		final String lowerCaseSql = sql.toLowerCase(Locale.ROOT);
		if ( lowerCaseSql.startsWith( "create table" ) ) {
			return formatCreateTable( sql );
		}
		else if ( lowerCaseSql.startsWith( "create index" )
				|| lowerCaseSql.startsWith( "create unique" ) ) {
			return formatAlterTable( sql );
		}
		else if ( lowerCaseSql.startsWith( "create" ) ) {
			return INITIAL_LINE + sql;
		}
		else if ( lowerCaseSql.startsWith( "alter table" ) ) {
			return formatAlterTable( sql );
		}
		else if ( lowerCaseSql.startsWith( "comment on" ) ) {
			return formatCommentOn( sql );
		}
		else {
			return INITIAL_LINE + sql;
		}
	}

	private String formatCommentOn(String sql) {
		final StringBuilder result = new StringBuilder( 60 ).append( INITIAL_LINE );
		final StringTokenizer tokens =
				new StringTokenizer( sql.replace('\n',' '),
						" '[]\"", true );

		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			final String token = tokens.nextToken();
			result.append( token );
			if ( isQuote( token ) ) {
				quoted = !quoted;
			}
			else if ( !quoted ) {
				if ( "is".equals( token ) ) {
					result.append( OTHER_LINES );
				}
			}
		}

		return result.toString();
	}

	private String formatAlterTable(String sql) {
		final StringBuilder result = new StringBuilder( 60 ).append( INITIAL_LINE );
		final StringTokenizer tokens =
				new StringTokenizer( sql.replace('\n',' '),
						" (,)'[]\"", true );

		boolean first = true;
		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			final String token = tokens.nextToken();
			if ( isQuote( token ) ) {
				quoted = !quoted;
			}
			else if ( !quoted ) {
				if ( !first && isBreak( token ) ) {
					result.append( OTHER_LINES );
				}
			}
			result.append( token );
			first = false;
		}

		return result.toString();
	}

	private String formatCreateTable(String sql) {
		final StringBuilder result = new StringBuilder( 60 ).append( INITIAL_LINE );
		final StringTokenizer tokens =
				new StringTokenizer( sql.replace('\n',' '),
						"(,)'[]\"", true );

		int depth = 0;
		boolean quoted = false;
		while ( tokens.hasMoreTokens() ) {
			final String token = tokens.nextToken();
			if ( isQuote( token ) ) {
				quoted = !quoted;
				result.append( token );
			}
			else if ( quoted ) {
				result.append( token );
			}
			else {
				if ( ")".equals( token ) ) {
					depth--;
					if ( depth == 0 ) {
						result.append( INITIAL_LINE );
					}
				}
				result.append( token );
				if ( ",".equals( token ) && depth == 1 ) {
					result.append( OTHER_LINES );
				}
				if ( "(".equals( token ) ) {
					depth++;
					if ( depth == 1 ) {
						result.append( OTHER_LINES ).append(' ');
					}
				}
			}
		}

		return result.toString();
	}

	private static boolean isBreak(String token) {
		return BREAKS.contains( token );
	}

	private static boolean isQuote(String token) {
		return QUOTES.contains( token );
	}

}
