/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.Objects;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import jakarta.persistence.criteria.Nulls;

/**
 * @author Steve Ebersole
 */
public class SqmSortSpecification implements JpaOrder {
	@SuppressWarnings("rawtypes")
	private final SqmExpression sortExpression;
	private final SortDirection sortOrder;
	private final boolean ignoreCase;
	private Nulls nullPrecedence;

	public SqmSortSpecification(
			@SuppressWarnings("rawtypes") SqmExpression sortExpression,
			SortDirection sortOrder,
			Nulls nullPrecedence) {
		this( sortExpression, sortOrder, nullPrecedence, false );
	}

	public SqmSortSpecification(
				SqmExpression<?> sortExpression,
				SortDirection sortOrder,
				Nulls nullPrecedence,
				boolean ignoreCase) {
		assert sortExpression != null;
		assert sortOrder != null;
		assert nullPrecedence != null;
		this.sortExpression = sortExpression;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
		this.ignoreCase = ignoreCase;
	}

	/**
	 * @deprecated Use {@link SqmSortSpecification#SqmSortSpecification(SqmExpression, SortDirection, Nulls)} instead
	 */
	@Deprecated
	public SqmSortSpecification(
			@SuppressWarnings("rawtypes") SqmExpression sortExpression,
			SortDirection sortOrder,
			NullPrecedence nullPrecedence) {
		this( sortExpression, sortOrder, nullPrecedence.getJpaValue() );
	}

	@SuppressWarnings("rawtypes")
	public SqmSortSpecification(SqmExpression sortExpression) {
		this( sortExpression, SortDirection.ASCENDING, Nulls.NONE );
	}

	@SuppressWarnings("rawtypes")
	public SqmSortSpecification(SqmExpression sortExpression, SortDirection sortOrder) {
		this( sortExpression, sortOrder, Nulls.NONE );
	}

	public SqmSortSpecification copy(SqmCopyContext context) {
		return new SqmSortSpecification( sortExpression.copy( context ), sortOrder, nullPrecedence, ignoreCase );
	}

	public SqmExpression<?> getSortExpression() {
		return sortExpression;
	}

	@Override
	public SortDirection getSortDirection() {
		return sortOrder;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public JpaOrder nullPrecedence(Nulls nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
		return this;
	}

	@Override
	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}

	@Override
	public JpaOrder reverse() {
		SortDirection newSortOrder = this.sortOrder == null ? SortDirection.DESCENDING : sortOrder.reverse();
		return new SqmSortSpecification( sortExpression, newSortOrder, nullPrecedence, ignoreCase );
	}

	@Override
	public JpaExpression<?> getExpression() {
		return getSortExpression();
	}

	@Override
	public boolean isAscending() {
		return sortOrder == SortDirection.ASCENDING;
	}

	public void appendHqlString(StringBuilder sb, SqmRenderContext context) {
		sortExpression.appendHqlString( sb, context );
		if ( sortOrder == SortDirection.DESCENDING ) {
			sb.append( " desc" );
			if ( nullPrecedence != null ) {
				if ( nullPrecedence == Nulls.FIRST ) {
					sb.append( " nulls first" );
				}
				else {
					sb.append( " nulls last" );
				}
			}
		}
		else if ( nullPrecedence != null ) {
			sb.append( " asc" );
			if ( nullPrecedence == Nulls.FIRST ) {
				sb.append( " nulls first" );
			}
			else {
				sb.append( " nulls last" );
			}
		}
	}

	@Override
	public boolean equals(Object other) {
		if ( this == other ) {
			return true;
		}
		// used in SqmInterpretationsKey.equals()
		return other instanceof SqmSortSpecification that
			&& Objects.equals( this.sortExpression, that.sortExpression )
			&& this.sortOrder == that.sortOrder
			&& this.nullPrecedence == that.nullPrecedence;
	}

	@Override
	public int hashCode() {
		return Objects.hash( sortExpression, sortOrder, nullPrecedence );
	}
}
