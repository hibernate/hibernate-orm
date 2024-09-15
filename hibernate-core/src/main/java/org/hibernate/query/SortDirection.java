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
		return switch (this) {
			case ASCENDING -> DESCENDING;
			case DESCENDING -> ASCENDING;
		};
	}

	public static SortDirection interpret(String value) {
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
