/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * The SQL set operators.
 *
 * @apiNote This is an SPI type. It should never occur
 * in APIs visible to the application program.
 *
 * @author Christian Beikov
 */
public enum SetOperator {
	/**
	 * Union of sets that removes duplicate rows.
	 */
	UNION,
	/**
	 * Union of bags that retains all elements.
	 */
	UNION_ALL,
	/**
	 * Intersection of sets that removes duplicate rows.
	 */
	INTERSECT,
	/**
	 * Intersection of bags that retains duplicate matches.
	 */
	INTERSECT_ALL,
	/**
	 * Exclusion of set elements of the set on the right-hand side.
	 */
	EXCEPT,
	/**
	 * Exclusion of bag elements of the bag on the right-hand side that retains duplicates.
	 */
	EXCEPT_ALL;

	public String sqlString() {
		return switch (this) {
			case UNION -> "union";
			case UNION_ALL -> "union all";
			case INTERSECT -> "intersect";
			case INTERSECT_ALL -> "intersect all";
			case EXCEPT -> "except";
			case EXCEPT_ALL -> "except all";
		};
	}
}
