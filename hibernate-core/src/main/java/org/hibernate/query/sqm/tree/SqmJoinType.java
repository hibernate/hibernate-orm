/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree;

import org.hibernate.sql.ast.JoinType;

/**
 * Represents a canonical join type.
 * <p/>
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
	INNER( "inner", JoinType.INNER ),

	/**
	 * Represents a left outer join.
	 */
	LEFT( "left outer", JoinType.LEFT ),

	/**
	 * Represents a right outer join.
	 */
	RIGHT( "right outer", JoinType.RIGHT ),

	/**
	 * Represents a cross join (aka a cartesian product).
	 */
	CROSS( "cross", JoinType.CROSS ),

	/**
	 * Represents a full join.
	 */
	FULL( "full", JoinType.FULL );

	private final String text;
	private final JoinType correspondingSqlJoinType;

	SqmJoinType(String text, JoinType correspondingSqlJoinType) {
		this.text = text;
		this.correspondingSqlJoinType = correspondingSqlJoinType;
	}

	@Override
	public String toString() {
		return text;
	}

	public String getText() {
		return text;
	}

	public JoinType getCorrespondingSqlJoinType() {
		return correspondingSqlJoinType;
	}
}
