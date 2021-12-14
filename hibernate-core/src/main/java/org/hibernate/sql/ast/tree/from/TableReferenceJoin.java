/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAstTreeHelper;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateContainer;

/**
 * Represents a join to a {@link TableReference}; roughly equivalent to a SQL join.
 *
 * @author Steve Ebersole
 */
public class TableReferenceJoin implements TableJoin, PredicateContainer {
	private final SqlAstJoinType sqlAstJoinType;
	private final NamedTableReference joinedTableBinding;
	private Predicate predicate;

	public TableReferenceJoin(SqlAstJoinType sqlAstJoinType, NamedTableReference joinedTableBinding, Predicate predicate) {
		this.sqlAstJoinType = sqlAstJoinType == null ? SqlAstJoinType.LEFT : sqlAstJoinType;
		this.joinedTableBinding = joinedTableBinding;
		this.predicate = predicate;

//		if ( joinType == JoinType.CROSS ) {
//			if ( predicate != null ) {
//				throw new IllegalJoinSpecificationException( "Cross join cannot include join predicate" );
//			}
//		}
	}

	@Override
	public SqlAstJoinType getJoinType() {
		return sqlAstJoinType;
	}

	public NamedTableReference getJoinedTableReference() {
		return joinedTableBinding;
	}

	@Override
	public SqlAstNode getJoinedNode() {
		return joinedTableBinding;
	}

	@Override
	public Predicate getPredicate() {
		return predicate;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableReferenceJoin( this );
	}

	@Override
	public String toString() {
		return getJoinType().getText() + " join " + getJoinedTableReference().toString();
	}

	@Override
	public void applyPredicate(Predicate newPredicate) {
		predicate = SqlAstTreeHelper.combinePredicates( predicate, newPredicate);
	}
}
