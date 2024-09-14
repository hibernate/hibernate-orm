/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import org.hibernate.dialect.NullOrdering;

import jakarta.persistence.criteria.Nulls;

/**
 * Enumerates the possibilities for the precedence of null values within
 * query result sets sorted by an {@code ORDER BY} clause.
 *
 * @author Lukasz Antoniak
 *
 * @deprecated Use Jakarta Persistence {@linkplain Nulls} instead.
 */
@Deprecated(since="7")
public enum NullPrecedence {
	/**
	 * Null precedence not specified. Relies on the RDBMS implementation.
	 */
	NONE,
	/**
	 * Null values appear at the beginning of the sorted collection.
	 */
	FIRST,
	/**
	 * Null values appear at the end of the sorted collection.
	 */
	LAST;

	/**
	 * Is this null precedence the default for the given sort order and null ordering.
	 *
	 * @deprecated No longer called
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public boolean isDefaultOrdering(SortDirection sortOrder, NullOrdering nullOrdering) {
		return switch (this) {
			case NONE -> true;
			case FIRST -> switch (nullOrdering) {
				case FIRST -> true;
				case LAST -> false;
				case SMALLEST -> sortOrder == SortDirection.ASCENDING;
				case GREATEST -> sortOrder == SortDirection.DESCENDING;
			};
			case LAST -> switch (nullOrdering) {
				case LAST -> true;
				case FIRST -> false;
				case SMALLEST -> sortOrder == SortDirection.DESCENDING;
				case GREATEST -> sortOrder == SortDirection.ASCENDING;
			};
		};
	}

	/**
	 * Interprets a string representation of a NullPrecedence, returning {@code null} by default.  For
	 * alternative default handling, see {@link #parse(String, NullPrecedence)}
	 *
	 * @param name The String representation to interpret
	 *
	 * @return The recognized NullPrecedence, or {@code null}
	 */
	public static NullPrecedence parse(String name) {
		for ( NullPrecedence value : values() ) {
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
	public static NullPrecedence parse(String name, NullPrecedence defaultValue) {
		final NullPrecedence value = parse( name );
		return value != null ? value : defaultValue;
	}

	public Nulls getJpaValue() {
		return switch (this) {
			case NONE -> Nulls.NONE;
			case FIRST -> Nulls.FIRST;
			case LAST -> Nulls.LAST;
		};
	}

	public static NullPrecedence fromJpaValue(Nulls jpaValue) {
		return switch (jpaValue) {
			case NONE -> NullPrecedence.NONE;
			case FIRST -> NullPrecedence.FIRST;
			case LAST -> NullPrecedence.LAST;
		};

	}
}
