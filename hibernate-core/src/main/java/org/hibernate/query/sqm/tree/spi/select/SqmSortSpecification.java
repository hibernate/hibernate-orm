/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.spi.select;

import jakarta.annotation.Nullable;
import java.util.Objects;

import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.sqm.tree.spi.SqmCacheable;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.SqmRenderContext;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Nulls;

/**
 * @author Steve Ebersole
 */
public class SqmSortSpecification implements JpaOrder, SqmCacheable {
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

	@Nonnull
	@Override
	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}

	@Nonnull
	@Override
	public JpaOrder reverse() {
		SortDirection newSortOrder = this.sortOrder == null ? SortDirection.DESCENDING : sortOrder.reverse();
		return new SqmSortSpecification( sortExpression, newSortOrder, nullPrecedence, ignoreCase );
	}

	@Nonnull
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
	public boolean equals(@Nullable Object other) {
		return other instanceof SqmSortSpecification that
			&& sortExpression.equals( that.sortExpression )
			&& this.sortOrder == that.sortOrder
			&& this.nullPrecedence == that.nullPrecedence;
	}

	@Override
	public int hashCode() {
		int result = sortExpression.hashCode();
		result = 31 * result + sortOrder.hashCode();
		result = 31 * result + Objects.hashCode( nullPrecedence );
		return result;
	}

	@Override
	public boolean isCompatible(Object other) {
		return other instanceof SqmSortSpecification that
			&& sortExpression.isCompatible( that.sortExpression )
			&& this.sortOrder == that.sortOrder
			&& this.nullPrecedence == that.nullPrecedence;
	}

	@Override
	public int cacheHashCode() {
		int result = sortExpression.cacheHashCode();
		result = 31 * result + sortOrder.hashCode();
		result = 31 * result + Objects.hashCode( nullPrecedence );
		return result;
	}
}
