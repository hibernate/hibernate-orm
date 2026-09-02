/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi;

import java.util.regex.Pattern;

import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

/// Escapes text embedded in SQL comments.
///
/// @author Steve Ebersole
/// @since 8.0
@SPI(USE)
public final class SqlComments {
	private static final Pattern CLOSING_COMMENT = Pattern.compile( "\\*/" );
	private static final Pattern OPENING_COMMENT = Pattern.compile( "/\\*" );

	private SqlComments() {
	}

	/// Escape nested opening and closing comment delimiters while preserving
	/// `null` and empty input.
	public static @Nullable String escape(@Nullable String comment) {
		if ( isNotEmpty( comment ) ) {
			final String escaped = CLOSING_COMMENT.matcher( comment ).replaceAll( "*\\\\/" );
			return OPENING_COMMENT.matcher( escaped ).replaceAll( "/\\\\*" );
		}
		return comment;
	}
}
