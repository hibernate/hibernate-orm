/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.select;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

import jakarta.persistence.criteria.Nulls;

/**
 * @author Steve Ebersole
 */
public class SortSpecification implements SqlAstNode {
	private final Expression sortExpression;
	private final SortDirection sortOrder;
	private final Nulls nullPrecedence;
	private final boolean ignoreCase;

	public SortSpecification(Expression sortExpression, SortDirection sortOrder) {
		this( sortExpression, sortOrder, Nulls.NONE, false );
	}

	public SortSpecification(Expression sortExpression, SortDirection sortOrder, Nulls nullPrecedence) {
		this( sortExpression, sortOrder, nullPrecedence, false );
	}

	/**
	 * @deprecated Use {@linkplain #SortSpecification(Expression, SortDirection, Nulls)} instead
	 */
	@Deprecated(since = "7", forRemoval = true)
	public SortSpecification(Expression sortExpression, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		this( sortExpression, sortOrder, nullPrecedence.getJpaValue() );
	}

	public SortSpecification(Expression sortExpression, SortDirection sortOrder, Nulls nullPrecedence, boolean ignoreCase) {
		assert sortExpression != null;
		assert sortOrder != null;
		assert nullPrecedence != null;
		this.sortExpression = sortExpression;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
		this.ignoreCase = ignoreCase;
	}

	public Expression getSortExpression() {
		return sortExpression;
	}

	public SortDirection getSortOrder() {
		return sortOrder;
	}

	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSortSpecification( this );
	}
}
