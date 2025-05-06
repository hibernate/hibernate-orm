/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;

import org.hibernate.engine.jdbc.env.spi.AnsiSqlKeywords;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.StringTokenizer;

import static org.hibernate.internal.util.StringHelper.WHITESPACE;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * Performs basic syntax highlighting for SQL using ANSI escape codes.
 *
 * @author Gavin King
 */
public final class HighlightingFormatter implements Formatter {

	private static final Set<String> KEYWORDS_LOWERCASED = new HashSet<>( new AnsiSqlKeywords().sql2003() );
	static {
		// additional keywords not reserved by ANSI SQL 2003
		KEYWORDS_LOWERCASED.addAll( Arrays.asList( "key", "sequence", "cascade", "increment", "boolean", "offset", "first", "next", "returning" ) );
	}

	public static final Formatter INSTANCE =
			new HighlightingFormatter(
					"34", // blue
					"36", // cyan
					"32"
			);
	private static final String SYMBOLS_AND_WS = "=><!+-*/()',.|&`\"?" + WHITESPACE;

	private static String escape(String code) {
		return "\u001b[" + code + "m";
	}

	private final String keywordEscape;
	private final String stringEscape;
	private final String quotedEscape;
	private final String normalEscape;

	/**
	 * @param keywordCode the ANSI escape code to use for highlighting SQL keywords
	 * @param stringCode the ANSI escape code to use for highlighting SQL strings
	 */
	public HighlightingFormatter(String keywordCode, String stringCode, String quotedCode) {
		keywordEscape = escape( keywordCode );
		stringEscape = escape( stringCode );
		quotedEscape = escape( quotedCode );
		normalEscape = escape( "0" );
	}

	@Override
	public String format(String sql) {
		if ( isEmpty( sql ) ) {
			return sql;
		}

		StringBuilder result = new StringBuilder();
		boolean inString = false;
		boolean inQuoted = false;
		for ( StringTokenizer tokenizer = new StringTokenizer( sql, SYMBOLS_AND_WS, true );
				tokenizer.hasMoreTokens(); ) {
			String token = tokenizer.nextToken();
			switch ( token ) {
				case "\"":
				case "`": // for MySQL
					if ( inString ) {
						result.append( token );
					}
					else if ( inQuoted ) {
						inQuoted = false;
						result.append( token ).append( normalEscape );
					}
					else {
						inQuoted = true;
						result.append( quotedEscape ).append( token );
					}
					break;
				case "'":
					if ( inQuoted ) {
						result.append( '\'' );
					}
					else if ( inString ) {
						inString = false;
						result.append( '\'' ).append( normalEscape );
					}
					else {
						inString = true;
						result.append( stringEscape ).append( '\'' );
					}
					break;
				default:
					if ( !inQuoted && KEYWORDS_LOWERCASED.contains( token.toLowerCase( Locale.ROOT ) ) ) {
						result.append( keywordEscape ).append( token ).append( normalEscape );
					}
					else {
						result.append( token );
					}
			}
		}
		return result.toString();
	}

}
