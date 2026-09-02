/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.queryhint.spi;

import java.util.regex.Pattern;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Provides reusable SQL query-hint rendering operations.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class QueryHints {
	private static final Pattern QUERY_PATTERN = Pattern.compile(
			"^\\s*(select\\s.+?\\sfrom\\s.+?)(\\s(?:(?:natural)?\\s*(?:left|right|full)?\\s*(?:inner|outer|cross)?\\s*join|straight_join)\\s.+?)?(\\swhere\\s.+?)?(\\sorder\\s+by\\s.+?)?$",
			Pattern.CASE_INSENSITIVE
	);

	private QueryHints() {
	}

	/// Insert a `USE INDEX` clause after the initial SELECT/FROM table segment.
	/// Return the original SQL when its shape is not recognized.
	public static String addUseIndexHint(String sql, String hints) {
		final var matcher = QUERY_PATTERN.matcher( sql );
		if ( matcher.matches() && matcher.groupCount() > 1 ) {
			final String startToken = matcher.group( 1 );
			return startToken + " use index (" + hints + ")"
					+ sql.substring( startToken.length() );
		}
		return sql;
	}
}
