/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import org.hibernate.AssertionFailure;
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
		switch (precedence) {
			case NONE:
				return true;
			case FIRST:
				switch ( nullOrdering ) {
					case FIRST:
						return true;
					case LAST:
						return false;
					case SMALLEST:
						return sortOrder == SortDirection.ASCENDING;
					case GREATEST:
						return sortOrder == SortDirection.DESCENDING;
					default:
						throw new AssertionFailure( "Unrecognized NullOrdering");
				}
			case LAST:
				switch ( nullOrdering ) {
					case LAST:
						return true;
					case FIRST:
						return false;
					case SMALLEST:
						return sortOrder == SortDirection.DESCENDING;
					case GREATEST:
						return sortOrder == SortDirection.ASCENDING;
					default:
						throw new AssertionFailure("Unrecognized NullOrdering");
				}
			default:
				throw new AssertionFailure("Unrecognized NullPrecedence");
		}
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
		for ( Nulls value : Nulls.values() ) {
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
		final Nulls value = parse( name );
		return value != null ? value : defaultValue;
	}

	private NullPrecedenceHelper() {
	}
}
