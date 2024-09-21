/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.sql.ast.SqlAstJoinType;

/**
 * Represents a canonical join type.
 * <p>
 * Note that currently HQL really only supports inner and left outer joins
 * (though cross joins can also be achieved).  This is because joins in HQL
 * are always defined in relation to a mapped association.  However, when we
 * start allowing users to specify ad-hoc joins this may need to change to
 * allow the full spectrum of join types.  Thus the others are provided here
 * currently just for completeness and for future expansion.
 *
 * @author Steve Ebersole
 */
public enum SqmJoinType {
	/**
	 * Represents an inner join.
	 */
	INNER( "inner", SqlAstJoinType.INNER, jakarta.persistence.criteria.JoinType.INNER ),

	/**
	 * Represents a left outer join.
	 */
	LEFT( "left outer", SqlAstJoinType.LEFT, jakarta.persistence.criteria.JoinType.LEFT ),

	/**
	 * Represents a right outer join.
	 */
	RIGHT( "right outer", SqlAstJoinType.RIGHT, jakarta.persistence.criteria.JoinType.RIGHT ),

	/**
	 * Represents a cross join (aka a cartesian product).
	 */
	CROSS( "cross", SqlAstJoinType.CROSS, null ),

	/**
	 * Represents a full join.
	 */
	FULL( "full", SqlAstJoinType.FULL, null );

	private final String text;
	private final SqlAstJoinType correspondingSqlAstJoinType;
	private final jakarta.persistence.criteria.JoinType correspondingJpaJoinType;

	SqmJoinType(
			String text,
			SqlAstJoinType correspondingSqlAstJoinType,
			jakarta.persistence.criteria.JoinType correspondingJpaJoinType) {
		this.text = text;
		this.correspondingSqlAstJoinType = correspondingSqlAstJoinType;
		this.correspondingJpaJoinType = correspondingJpaJoinType;
	}

	@Override
	public String toString() {
		return text;
	}

	public String getText() {
		return text;
	}

	public SqlAstJoinType getCorrespondingSqlJoinType() {
		return correspondingSqlAstJoinType;
	}

	public jakarta.persistence.criteria.JoinType getCorrespondingJpaJoinType() {
		return correspondingJpaJoinType;
	}

	@SuppressWarnings("DuplicateBranchesInSwitch")
	public static SqmJoinType from(jakarta.persistence.criteria.JoinType jpaJoinType) {
		switch ( jpaJoinType ) {
			case INNER: {
				return INNER;
			}
			case LEFT: {
				return LEFT;
			}
			case RIGHT: {
				return RIGHT;
			}
			default: {
				// generally speaking, the default for JPA JoinType is INNER
				return INNER;
			}
		}
	}
}
