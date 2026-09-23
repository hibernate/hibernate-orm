/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.materialize;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.AnnotationException;
import org.hibernate.boot.model.naming.spi.IndexTermNamingInput.Order;

/// Separates index terms and ordering without interpreting SQL expressions.
///
/// @author Steve Ebersole
public final class IndexColumnList {
	private IndexColumnList() {}

	/// A trimmed source term with its top-level ordering removed.
	///
	/// @author Steve Ebersole
	public record Term(String text, Order order, boolean expression) {}

	public static List<Term> parse(String source, String location) {
		if ( source == null || source.isBlank() ) {
			return List.of();
		}
		final var result = new ArrayList<Term>();
		int depth = 0;
		char quote = 0;
		int start = 0;
		for ( int i = 0; i < source.length(); i++ ) {
			final char c = source.charAt( i );
			if ( quote != 0 ) {
				if ( c == quote ) {
					if ( i + 1 < source.length() && source.charAt( i + 1 ) == quote ) { i++; }
					else { quote = 0; }
				}
			}
			else if ( c == '\'' || c == '"' || c == '`' || c == '[' ) {
				quote = c == '[' ? ']' : c;
			}
			else if ( c == '(' ) { depth++; }
			else if ( c == ')' ) {
				if ( --depth < 0 ) { throw malformed( source, location ); }
			}
			else if ( c == ',' && depth == 0 ) {
				result.add( term( source.substring( start, i ), location ) );
				start = i + 1;
			}
		}
		if ( quote != 0 || depth != 0 ) { throw malformed( source, location ); }
		result.add( term( source.substring( start ), location ) );
		return List.copyOf( result );
	}

	private static Term term(String source, String location) {
		String text = source.trim();
		Order order = Order.UNSPECIFIED;
		int wordStart = text.length();
		while ( wordStart > 0 && !Character.isWhitespace( text.charAt( wordStart - 1 ) ) ) { wordStart--; }
		if ( wordStart > 0 ) {
			final String word = text.substring( wordStart ).toUpperCase( Locale.ROOT );
			if ( word.equals( "ASC" ) || word.equals( "DESC" ) ) {
				order = Order.valueOf( word );
				text = text.substring( 0, wordStart ).trim();
			}
		}
		if ( text.isEmpty() ) { throw malformed( source, location ); }
		return new Term( text, order, text.startsWith( "(" ) );
	}

	private static AnnotationException malformed(String source, String location) {
		return new AnnotationException( "Malformed @Index columnList '" + source + "' at " + location );
	}
}
