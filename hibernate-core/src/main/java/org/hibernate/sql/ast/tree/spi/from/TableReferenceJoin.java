/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.produce.IllegalJoinSpecificationException;
import org.hibernate.query.sqm.tree.SqmJoinType;

/**
 * Represents a join to a {@link TableReference}; roughly equivalent to a SQL join.
 *
 * @author Steve Ebersole
 */
public class TableReferenceJoin {
	private final SqmJoinType joinType;
	private final TableReference joinedTableBinding;
	private final Predicate predicate;

	public TableReferenceJoin(SqmJoinType joinType, TableReference joinedTableBinding, Predicate predicate) {
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

	public TableReference getJoinedTableBinding() {
		return joinedTableBinding;
	}

	public Predicate getJoinPredicate() {
		return predicate;
	}
}
