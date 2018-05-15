/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.internal.util.Loggable;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.IllegalJoinSpecificationException;
import org.hibernate.sql.ast.tree.spi.predicate.Predicate;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;

/**
 * Represents a join to a {@link TableReference}; roughly equivalent to a SQL join.
 *
 * @author Steve Ebersole
 */
public class TableReferenceJoin implements SqlAstNode, Loggable {
	private final JoinType joinType;
	private final TableReference joinedTableBinding;
	private final Predicate predicate;

	public TableReferenceJoin(JoinType joinType, TableReference joinedTableBinding, Predicate predicate) {
		this.joinType = joinType;
		this.joinedTableBinding = joinedTableBinding;
		this.predicate = predicate;

		if ( joinType == JoinType.CROSS ) {
			if ( predicate != null ) {
				throw new IllegalJoinSpecificationException( "Cross join cannot include join predicate" );
			}
		}
	}

	public JoinType getJoinType() {
		return joinType;
	}

	public TableReference getJoinedTableReference() {
		return joinedTableBinding;
	}

	public Predicate getJoinPredicate() {
		return predicate;
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableReferenceJoin( this );
	}

	@Override
	public String toLoggableFragment() {
		return getJoinType().getText() + " join " + getJoinedTableReference().toLoggableFragment();
	}
}
