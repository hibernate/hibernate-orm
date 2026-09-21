/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.spi;

/// Validates identifier text independently of mapping interpretation and dialect syntax.
///
/// @author Steve Ebersole
final class NameText {
	private NameText() {
	}

	static void validate(String text) {
		if ( text == null || text.isEmpty() ) {
			throw new IllegalArgumentException( "Name text must not be null or empty" );
		}
		if ( text.length() > 2 ) {
			final char last = text.charAt( text.length() - 1 );
			final boolean delimited = switch ( text.charAt( 0 ) ) {
				case '`' -> last == '`';
				case '"' -> last == '"';
				case '[' -> last == ']';
				default -> false;
			};
			if ( delimited ) {
				throw new IllegalArgumentException( "Name text must not contain surrounding quote delimiters" );
			}
		}
	}
}
