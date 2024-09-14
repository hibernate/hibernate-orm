/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

/**
 * @author Christian Beikov
 */
public class QueryLiteralHelper {
	private QueryLiteralHelper() {
		// disallow direct instantiation
	}

	public static String toStringLiteral(String value) {
		final StringBuilder sb = new StringBuilder( value.length() + 2 );
		appendStringLiteral( sb, value );
		return sb.toString();
	}

	public static void appendStringLiteral(StringBuilder sb, String value) {
		sb.append( '\'' );
		for ( int i = 0; i < value.length(); i++ ) {
			final char c = value.charAt( i );
			if ( c == '\'' ) {
				sb.append( '\'' );
			}
			sb.append( c );
		}
		sb.append( '\'' );
	}

}
