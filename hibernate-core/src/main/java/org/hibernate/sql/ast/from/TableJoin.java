/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.from;

import org.hibernate.sql.ast.predicate.Predicate;
import org.hibernate.sql.convert.IllegalJoinSpecificationException;
import org.hibernate.sqm.query.SqmJoinType;

/**
 * Represents a join to a {@link TableBinding}; roughly equivalent to a SQL join.
 *
 * @author Steve Ebersole
 */
public class TableJoin {
	private final SqmJoinType joinType;
	private final TableBinding joinedTableBinding;
	private final Predicate predicate;

	public TableJoin(SqmJoinType joinType, TableBinding joinedTableBinding, Predicate predicate) {
		this.joinType = joinType;
		this.joinedTableBinding = joinedTableBinding;
		this.predicate = predicate;

		if ( joinType == SqmJoinType.CROSS ) {
			if ( predicate != null ) {
				throw new IllegalJoinSpecificationException( "Cross join cannot include join predicate" );
			}
		}
	}

	public SqmJoinType getJoinType() {
		return joinType;
	}

	public TableBinding getJoinedTableBinding() {
		return joinedTableBinding;
	}

	public Predicate getJoinPredicate() {
		return predicate;
	}
}
