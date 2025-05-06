/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;


public final class QuotingHelper {

	private QuotingHelper() { /* static methods only - hide constructor */
	}

	public static String unquoteIdentifier(String text) {
		final int end = text.length() - 1;
		assert text.charAt( 0 ) == '`' && text.charAt( end ) == '`';
		// Unquote a parsed quoted identifier and handle escape sequences
		final StringBuilder sb = new StringBuilder( text.length() - 2 );
		for ( int i = 1; i < end; i++ ) {
			char c = text.charAt( i );
			switch ( c ) {
				case '\\':
					if ( ( i + 1 ) < end ) {
						char nextChar = text.charAt( ++i );
						switch ( nextChar ) {
							case 'b':
								c = '\b';
								break;
							case 't':
								c = '\t';
								break;
							case 'n':
								c = '\n';
								break;
							case 'f':
								c = '\f';
								break;
							case 'r':
								c = '\r';
								break;
							case '\\':
								c = '\\';
								break;
							case '\'':
								c = '\'';
								break;
							case '"':
								c = '"';
								break;
							case '`':
								c = '`';
								break;
							case 'u':
								c = (char) Integer.parseInt( text.substring( i + 1, i + 5 ), 16 );
								i += 4;
								break;
							default:
								sb.append( '\\' );
								c = nextChar;
								break;
						}
					}
					break;
				default:
					break;
			}
			sb.append( c );
		}
		return sb.toString();
	}

	public static String unquoteStringLiteral(String text) {
		assert text.length() > 1;
		final int end = text.length() - 1;
		final char delimiter = text.charAt( 0 );
		assert delimiter == text.charAt( end );
		// Unescape the parsed literal
		final StringBuilder sb = new StringBuilder( text.length() - 2 );
		for ( int i = 1; i < end; i++ ) {
			char c = text.charAt( i );
			switch ( c ) {
				case '\'':
					if ( delimiter == '\'' ) {
						i++;
					}
					break;
				case '"':
					if ( delimiter == '"' ) {
						i++;
					}
					break;
				default:
					break;
			}
			sb.append( c );
		}
		return sb.toString();
	}

	public static String unquoteJavaStringLiteral(String text) {
		assert text.length() > 1;
		final char firstChar = text.charAt( 0 );
		final int start = firstChar == 'j' || firstChar == 'J' ? 1 : 0;
		final int end = text.length() - 1;
		final char delimiter = text.charAt( start );
		assert delimiter == text.charAt( end );
		// Handle escape sequences
		final StringBuilder sb = new StringBuilder( text.length() - ( start + 2 ) );
		for ( int i = start + 1; i < end; i++ ) {
			char c = text.charAt( i );
			if ( c == '\\' && ( i + 1 ) < end ) {
				char nextChar = text.charAt( ++i );
				switch ( nextChar ) {
					case 'b':
						c = '\b';
						break;
					case 't':
						c = '\t';
						break;
					case 'n':
						c = '\n';
						break;
					case 'f':
						c = '\f';
						break;
					case 'r':
						c = '\r';
						break;
					case '\\':
						c = '\\';
						break;
					case '\'':
						c = '\'';
						break;
					case '"':
						c = '"';
						break;
					case '`':
						c = '`';
						break;
					case 'u':
						c = (char) Integer.parseInt( text.substring( i + 1, i + 5 ), 16 );
						i += 4;
						break;
					default:
						sb.append( '\\' );
						c = nextChar;
						break;
				}
			}
			sb.append( c );
		}
		return sb.toString();
	}

	public static void appendDoubleQuoteEscapedString(StringBuilder sb, String text) {
		appendWithDoubleEscaping( sb, text, '"' );
	}

	public static void appendSingleQuoteEscapedString(StringBuilder sb, String text) {
		appendWithDoubleEscaping( sb, text, '\'' );
	}

	private static void appendWithDoubleEscaping(StringBuilder sb, String text, char quoteChar) {
		sb.append( quoteChar );
		for ( int i = 0; i < text.length(); i++ ) {
			final char c = text.charAt( i );
			if ( c == quoteChar ) {
				sb.append( quoteChar );
			}
			sb.append( c );
		}
		sb.append( quoteChar );
	}

}
