/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

import org.hibernate.query.sqm.NullOrdering;

/**
 * Defines precedence of null values within {@code ORDER BY} clause.
 *
 * @author Lukasz Antoniak
 */
public enum NullPrecedence {
	/**
	 * Null precedence not specified. Relies on the RDBMS implementation.
	 */
	NONE {
		@Override
		public boolean isDefaultOrdering(SortOrder sortOrder, NullOrdering nullOrdering) {
			return true;
		}
	},

	/**
	 * Null values appear at the beginning of the sorted collection.
	 */
	FIRST {
		@Override
		public boolean isDefaultOrdering(SortOrder sortOrder, NullOrdering nullOrdering) {
			switch ( nullOrdering ) {
				case FIRST:
					return true;
				case SMALLEST:
					return sortOrder == SortOrder.ASCENDING;
				case GREATEST:
					return sortOrder == SortOrder.DESCENDING;
			}
			return false;
		}
	},

	/**
	 * Null values appear at the end of the sorted collection.
	 */
	LAST {
		@Override
		public boolean isDefaultOrdering(SortOrder sortOrder, NullOrdering nullOrdering) {
			switch ( nullOrdering ) {
				case LAST:
					return true;
				case SMALLEST:
					return sortOrder == SortOrder.DESCENDING;
				case GREATEST:
					return sortOrder == SortOrder.ASCENDING;
			}
			return false;
		}
	};

	/**
	 * Is this null precedence the default for the given sort order and null ordering.
	 */
	public abstract boolean isDefaultOrdering(SortOrder sortOrder, NullOrdering nullOrdering);

	/**
	 * Interprets a string representation of a NullPrecedence, returning {@code null} by default.  For
	 * alternative default handling, see {@link #parse(String, NullPrecedence)}
	 *
	 * @param name The String representation to interpret
	 *
	 * @return The recognized NullPrecedence, or {@code null}
	 */
	public static NullPrecedence parse(String name) {
		if ( "none".equalsIgnoreCase( name ) ) {
			return NullPrecedence.NONE;
		}
		else if ( "first".equalsIgnoreCase( name ) ) {
			return NullPrecedence.FIRST;
		}
		else if ( "last".equalsIgnoreCase( name ) ) {
			return NullPrecedence.LAST;
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
}
