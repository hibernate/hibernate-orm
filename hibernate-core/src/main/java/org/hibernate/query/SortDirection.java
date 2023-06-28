/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query;

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

	public SortDirection reverse() {
		switch (this) {
			case ASCENDING:
				return DESCENDING;
			case DESCENDING:
				return ASCENDING;
			default:
				return this;
		}
	}

	public static SortDirection interpret(String value) {
		if ( value == null ) {
			return null;
		}

		switch ( value.toLowerCase(Locale.ROOT) ) {
			case "asc":
			case "ascending":
				return ASCENDING;
			case "desc":
			case "descending":
				return DESCENDING;
			default:
				throw new IllegalArgumentException( "Unknown sort order: " + value );
		}
	}
}
