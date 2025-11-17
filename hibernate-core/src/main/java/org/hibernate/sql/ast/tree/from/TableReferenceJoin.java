/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
	private final boolean innerJoin;
	private final NamedTableReference joinedTableBinding;
	private Predicate predicate;

	public TableReferenceJoin(boolean innerJoin, NamedTableReference joinedTableBinding, Predicate predicate) {
		this.innerJoin = innerJoin;
		this.joinedTableBinding = joinedTableBinding;
		this.predicate = predicate;
	}

	@Override
	public SqlAstJoinType getJoinType() {
		return innerJoin ? SqlAstJoinType.INNER : SqlAstJoinType.LEFT;
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
		return getJoinType().getText() + "join " + getJoinedTableReference().toString();
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public void applyPredicate(Predicate newPredicate) {
		predicate = SqlAstTreeHelper.combinePredicates( predicate, newPredicate);
	}
}
