/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.order;

/**
 * @author Steve Ebersole
 */
public enum SqmSortOrder {
	ASCENDING,
	DESCENDING;

	public static SqmSortOrder interpret(String value) {
		if ( value == null ) {
			return null;
		}

		if ( value.equalsIgnoreCase( "ascending" ) || value.equalsIgnoreCase( "asc" ) ) {
			return ASCENDING;
		}

		if ( value.equalsIgnoreCase( "descending" ) || value.equalsIgnoreCase( "desc" ) ) {
			return DESCENDING;
		}

		throw new IllegalArgumentException( "Unknown sort order : " + value );
	}
}
