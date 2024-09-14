/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm;

/**
 * The SQL set operators.
 *
 * @author Christian Beikov
 */
public enum SetOperator {
	/**
	 * Union of sets that removes duplicate rows.
	 */
	UNION("union"),
	/**
	 * Union of bags that retains all elements.
	 */
	UNION_ALL("union all"),
	/**
	 * Intersection of sets that removes duplicate rows.
	 */
	INTERSECT("intersect"),
	/**
	 * Intersection of bags that retains duplicate matches.
	 */
	INTERSECT_ALL("intersect all"),
	/**
	 * Exclusion of set elements of the set on the right-hand side.
	 */
	EXCEPT("except"),
	/**
	 * Exclusion of bag elements of the bag on the right-hand side that retains duplicates.
	 */
	EXCEPT_ALL("except all");

	private final String sqlString;

	private SetOperator(String sqlString) {
		this.sqlString = sqlString;
	}

	public String sqlString() {
		return sqlString;
	}
}
