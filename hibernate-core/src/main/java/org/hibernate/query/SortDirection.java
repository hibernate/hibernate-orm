/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Locale;

/**
 * Enumerates the directions in which query results may be sorted.
 *
 * @author Steve Ebersole
 *
 * @see Order
 */
public enum SortDirection {
	ASCENDING,
	DESCENDING;

	@Nonnull
	public SortDirection reverse() {
		return switch (this) {
			case ASCENDING -> DESCENDING;
			case DESCENDING -> ASCENDING;
		};
	}

	@Nullable
	public static SortDirection interpret(@Nullable String value) {
		if ( value == null ) {
			return null;
		}
		else {
			return switch ( value.toLowerCase(Locale.ROOT) ) {
				case "asc", "ascending" -> ASCENDING;
				case "desc", "descending" -> DESCENDING;
				default -> throw new IllegalArgumentException( "Unknown sort order: " + value );
			};
		}
	}
}
