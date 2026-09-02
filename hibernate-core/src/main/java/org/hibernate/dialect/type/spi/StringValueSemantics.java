/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.type.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Describes independent database semantics for empty strings and trailing
/// spaces in fixed-width character values.
///
/// @author Steve Ebersole
/// @since 8.0
/// @see Dialect#getStringValueSemantics()
@SPI({ USE, SUPPLY })
public record StringValueSemantics(
		EmptyStringSemantics emptyStringSemantics,
		CharTrailingSpaceSemantics charTrailingSpaceSemantics) {
	/// Standard SQL string semantics.
	public static final StringValueSemantics STANDARD = new StringValueSemantics(
			EmptyStringSemantics.DISTINCT,
			CharTrailingSpaceSemantics.PRESERVED
	);
	/// Empty strings are read and stored as null.
	public static final StringValueSemantics EMPTY_STRING_AS_NULL = new StringValueSemantics(
			EmptyStringSemantics.AS_NULL,
			CharTrailingSpaceSemantics.PRESERVED
	);
	/// Fixed-width character values lose trailing spaces.
	public static final StringValueSemantics CHAR_TRAILING_SPACES_STRIPPED = new StringValueSemantics(
			EmptyStringSemantics.DISTINCT,
			CharTrailingSpaceSemantics.STRIPPED
	);
	/// Empty strings become null and fixed-width values lose trailing spaces.
	public static final StringValueSemantics EMPTY_STRING_AS_NULL_AND_CHAR_TRAILING_SPACES_STRIPPED =
			new StringValueSemantics(
					EmptyStringSemantics.AS_NULL,
					CharTrailingSpaceSemantics.STRIPPED
			);

	/// Validate both semantic axes.
	public StringValueSemantics {
		if ( emptyStringSemantics == null || charTrailingSpaceSemantics == null ) {
			throw new IllegalArgumentException( "String-value semantic axes must not be null" );
		}
	}

	/// Whether an empty string is treated as null.
	public boolean treatsEmptyStringAsNull() {
		return emptyStringSemantics == EmptyStringSemantics.AS_NULL;
	}

	/// Whether fixed-width character values lose trailing spaces.
	public boolean stripsCharTrailingSpaces() {
		return charTrailingSpaceSemantics == CharTrailingSpaceSemantics.STRIPPED;
	}

	/// Empty-string behavior.
	public enum EmptyStringSemantics {
		DISTINCT,
		AS_NULL
	}

	/// Fixed-width character trailing-space behavior.
	public enum CharTrailingSpaceSemantics {
		PRESERVED,
		STRIPPED
	}
}
