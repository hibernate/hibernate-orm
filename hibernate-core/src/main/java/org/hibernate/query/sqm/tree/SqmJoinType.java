/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.query.common.JoinType;
import org.hibernate.sql.ast.SqlAstJoinType;

/**
 * Represents a canonical join type.
 *
 * @author Steve Ebersole
 *
 * @see JoinType
 * @see SqlAstJoinType
 */
public enum SqmJoinType {
	/**
	 * Represents an inner join.
	 */
	INNER,

	/**
	 * Represents a left outer join.
	 */
	LEFT,

	/**
	 * Represents a right outer join.
	 */
	RIGHT,

	/**
	 * Represents a cross join (aka a cartesian product).
	 */
	CROSS,

	/**
	 * Represents a full join.
	 */
	FULL;

	@Override
	public String toString() {
		return getText();
	}

	public String getText() {
		return switch (this) {
			case RIGHT -> "right outer";
			case LEFT -> "left outer";
			case INNER -> "inner";
			case FULL -> "full";
			case CROSS -> "cross";
		};
	}

	public SqlAstJoinType getCorrespondingSqlJoinType() {
		return switch (this) {
			case RIGHT -> SqlAstJoinType.RIGHT;
			case LEFT -> SqlAstJoinType.LEFT;
			case INNER -> SqlAstJoinType.INNER;
			case FULL -> SqlAstJoinType.FULL;
			case CROSS -> SqlAstJoinType.CROSS;
		};
	}

	public jakarta.persistence.criteria.JoinType getCorrespondingJpaJoinType() {
		return switch (this) {
			case RIGHT -> jakarta.persistence.criteria.JoinType.RIGHT;
			case LEFT -> jakarta.persistence.criteria.JoinType.LEFT;
			case INNER -> jakarta.persistence.criteria.JoinType.INNER;
			default -> null;
		};
	}

	public JoinType getCorrespondingJoinType() {
		return switch (this) {
			case RIGHT -> JoinType.RIGHT;
			case LEFT -> JoinType.LEFT;
			case INNER -> JoinType.INNER;
			case FULL -> JoinType.FULL;
			case CROSS -> JoinType.CROSS;
		};
	}

	public static SqmJoinType from(JoinType joinType) {
		return switch ( joinType ) {
			case INNER -> INNER;
			case LEFT -> LEFT;
			case RIGHT -> RIGHT;
			case CROSS -> CROSS;
			case FULL -> FULL;
		};
	}

	public static SqmJoinType from(jakarta.persistence.criteria.JoinType jpaJoinType) {
		return switch ( jpaJoinType ) {
			case INNER -> INNER;
			case LEFT -> LEFT;
			case RIGHT -> RIGHT;
		};
	}
}
