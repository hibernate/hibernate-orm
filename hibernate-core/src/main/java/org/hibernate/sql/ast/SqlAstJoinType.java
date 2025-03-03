/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast;

/**
 * @author Steve Ebersole
 */
public enum SqlAstJoinType {
	/**
	 * Represents an inner join.
	 */
	INNER( "" ),

	/**
	 * Represents a left outer join.
	 */
	LEFT( "left " ),

	/**
	 * Represents a right outer join.
	 */
	RIGHT( "right " ),

	/**
	 * Represents a cross join (that is, a Cartesian product).
	 */
	CROSS( "cross " ),

	/**
	 * Represents a full join.
	 */
	FULL( "full " );

	private final String text;

	SqlAstJoinType(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	public String getText() {
		return text;
	}
}
