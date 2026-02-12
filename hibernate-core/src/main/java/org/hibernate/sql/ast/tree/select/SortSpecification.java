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
import org.hibernate.sql.ast.tree.expression.SqlSelectionExpression;

import static org.hibernate.sql.ast.tree.expression.SqlTupleContainer.getSqlTuple;

/**
 * @author Steve Ebersole
 */
public class SortSpecification implements SqlAstNode {
	private final Expression sortExpression;
	private final SortDirection sortOrder;
	private final Nulls nullPrecedence;
	private final boolean ignoreCase;
	private final int[] sortSelectionIndexes;

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
		this.sortSelectionIndexes = sortSelectionIndexes( sortExpression );
	}

	private int[] sortSelectionIndexes(Expression expression) {
		final var sqlTuple = getSqlTuple( expression );
		if ( sqlTuple == null ) {
			return new int[] { sortSelectionIndex( expression ) };
		}
		else {
			final var expressions = sqlTuple.getExpressions();
			final int size = expressions.size();
			final int[] indexes = new int[size];
			for ( int i = 0; i < size; i++ ) {
				indexes[i] = sortSelectionIndex( expressions.get( i ) );
			}
			return indexes;
		}
	}

	private int sortSelectionIndex(Expression expression) {
		return expression instanceof SqlSelectionExpression selectionExpression
				? selectionExpression.getSelection().getValuesArrayPosition()
				: -1;
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

	public int[] getSortSelectionIndexes() {
		return sortSelectionIndexes;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitSortSpecification( this );
	}
}
