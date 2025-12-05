/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.dialect.NullOrdering;
import org.hibernate.query.SortDirection;

import jakarta.persistence.criteria.Nulls;

/**
 * @author Steve Ebersole
 */
public class NullPrecedenceHelper {

	/**
	 * Is this null precedence the default for the given sort order and null ordering.
	 */
	public static boolean isDefaultOrdering(
			Nulls precedence,
			SortDirection sortOrder,
			NullOrdering nullOrdering) {
		return switch ( precedence ) {
			case NONE -> true;
			case FIRST -> switch ( nullOrdering ) {
				case FIRST -> true;
				case LAST -> false;
				case SMALLEST -> sortOrder == SortDirection.ASCENDING;
				case GREATEST -> sortOrder == SortDirection.DESCENDING;
			};
			case LAST -> switch ( nullOrdering ) {
				case LAST -> true;
				case FIRST -> false;
				case SMALLEST -> sortOrder == SortDirection.DESCENDING;
				case GREATEST -> sortOrder == SortDirection.ASCENDING;
			};
		};
	}

	/**
	 * Interprets a string representation of a NullPrecedence, returning {@code null} by default.  For
	 * alternative default handling, see {@link #parse(String, Nulls)}
	 *
	 * @param name The String representation to interpret
	 *
	 * @return The recognized NullPrecedence, or {@code null}
	 */
	public static Nulls parse(String name) {
		for ( var value : Nulls.values() ) {
			if ( value.name().equalsIgnoreCase( name ) ) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Interprets a string representation of a NullPrecedence, returning the specified default if not recognized.
	 *
	 * @param name The String representation to interpret
	 * @param defaultValue The default value to use
	 *
	 * @return The recognized NullPrecedence, or {@code defaultValue}.
	 */
	public static Nulls parse(String name, Nulls defaultValue) {
		final var value = parse( name );
		return value != null ? value : defaultValue;
	}

	private NullPrecedenceHelper() {
	}
}
