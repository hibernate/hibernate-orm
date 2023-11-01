/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.AssertionFailure;
import org.hibernate.dialect.NullOrdering;

import jakarta.persistence.criteria.Nulls;

/**
 * Enumerates the possibilities for the precedence of null values within
 * query result sets sorted by an {@code ORDER BY} clause.
 *
 * @author Lukasz Antoniak
 */
public enum NullPrecedence {
	/**
	 * Null precedence not specified. Relies on the RDBMS implementation.
	 */
	NONE( Nulls.NONE ),
	/**
	 * Null values appear at the beginning of the sorted collection.
	 */
	FIRST( Nulls.FIRST ),
	/**
	 * Null values appear at the end of the sorted collection.
	 */
	LAST( Nulls.LAST );

	private final Nulls jpaValue;

	NullPrecedence(Nulls jpaValue) {
		this.jpaValue = jpaValue;
	}

	/**
	 * Is this null precedence the default for the given sort order and null ordering.
	 */
	public boolean isDefaultOrdering(SortDirection sortOrder, NullOrdering nullOrdering) {
		switch (this) {
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
						throw new AssertionFailure("Unrecognized NullOrdering");
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
		return jpaValue;
	}

	public static NullPrecedence fromJpaValue(Nulls jpaValue) {
		switch ( jpaValue ) {
			case NONE: {
				return NullPrecedence.NONE;
			}
			case FIRST: {
				return NullPrecedence.FIRST;
			}
			case LAST: {
				return NullPrecedence.LAST;
			}
		}

		throw new IllegalArgumentException( "Unexpected JPA Nulls - " + jpaValue );
	}
}
