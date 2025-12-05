/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.select;

import java.util.List;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaQueryPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCacheable;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmRenderContext;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

import static java.util.Collections.emptyList;

/**
 * Defines the ordering and fetch/offset part of a query which is shared with query groups.
 *
 * @author Christian Beikov
 */
public abstract class SqmQueryPart<T> implements SqmVisitableNode, JpaQueryPart<T> {
	private final NodeBuilder nodeBuilder;

	private @Nullable SqmOrderByClause orderByClause;

	private @Nullable SqmExpression<? extends Number> offsetExpression;
	private @Nullable SqmExpression<? extends Number> fetchExpression;
	private FetchClauseType fetchClauseType = FetchClauseType.ROWS_ONLY;

	public SqmQueryPart(NodeBuilder nodeBuilder) {
		this.nodeBuilder = nodeBuilder;
	}

	public SqmQueryPart(SqmQueryPart<T> original, SqmCopyContext context) {
		this.nodeBuilder = original.nodeBuilder;
		if ( original.orderByClause != null ) {
			this.orderByClause = original.orderByClause.copy( context );
		}
		if ( original.offsetExpression != null ) {
			this.offsetExpression = original.offsetExpression.copy( context );
		}
		if ( original.fetchExpression != null ) {
			this.fetchExpression = original.fetchExpression.copy( context );
		}
		this.fetchClauseType = original.fetchClauseType;
	}

	protected void copyTo(SqmQueryPart<T> target, SqmCopyContext context) {
		if ( orderByClause != null ) {
			target.orderByClause = orderByClause.copy( context );
		}
		if ( offsetExpression != null ) {
			target.offsetExpression = offsetExpression.copy( context );
		}
		if ( fetchExpression != null ) {
			target.fetchExpression = fetchExpression.copy( context );
		}
		target.fetchClauseType = fetchClauseType;
	}

	@Override
	public abstract SqmQueryPart<T> copy(SqmCopyContext context);

	public abstract SqmQuerySpec<T> getFirstQuerySpec();

	public abstract SqmQuerySpec<T> getLastQuerySpec();

	public abstract boolean isSimpleQueryPart();

	@Override
	public NodeBuilder nodeBuilder() {
		return nodeBuilder;
	}

	public @Nullable SqmOrderByClause getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(@Nullable SqmOrderByClause orderByClause) {
		this.orderByClause = orderByClause;
	}

	public @Nullable SqmExpression<? extends Number> getFetchExpression() {
		return fetchExpression;
	}

	public @Nullable SqmExpression<? extends Number> getOffsetExpression() {
		return offsetExpression;
	}

	public void setOffsetExpression(@Nullable SqmExpression<? extends Number> offsetExpression) {
		if ( offsetExpression != null ) {
			offsetExpression.applyInferableType( nodeBuilder.getIntegerType() );
		}
		this.offsetExpression = offsetExpression;
	}

	public void setFetchExpression(@Nullable SqmExpression<? extends Number> fetchExpression) {
		setFetchExpression( fetchExpression, FetchClauseType.ROWS_ONLY );
	}

	public void setFetchExpression(@Nullable SqmExpression<? extends Number> fetchExpression, FetchClauseType fetchClauseType) {
		if ( fetchExpression == null ) {
			this.fetchExpression = null;
			this.fetchClauseType = FetchClauseType.ROWS_ONLY;
		}
		else {
			if ( fetchClauseType == null ) {
				throw new IllegalArgumentException( "Fetch clause may not be null" );
			}
			fetchExpression.applyInferableType( nodeBuilder.getIntegerType() );
			this.fetchExpression = fetchExpression;
			this.fetchClauseType = fetchClauseType;
		}
	}

	@Override
	public FetchClauseType getFetchClauseType() {
		return fetchClauseType;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JPA

	@Override
	public List<SqmSortSpecification> getSortSpecifications() {
		final SqmOrderByClause orderByClause = getOrderByClause();
		return orderByClause == null ? emptyList() : orderByClause.getSortSpecifications();
	}

	@Override
	public SqmQueryPart<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		SqmOrderByClause orderByClause = getOrderByClause();
		if ( orderByClause == null ) {
			setOrderByClause( orderByClause = new SqmOrderByClause() );
		}

		//noinspection unchecked
		orderByClause.setSortSpecifications( (List<SqmSortSpecification>) sortSpecifications );

		return this;
	}

	@Override
	public @Nullable JpaExpression<? extends Number> getOffset() {
		return getOffsetExpression();
	}

	@Override
	public SqmQueryPart<T> setOffset(@Nullable JpaExpression<? extends Number> offset) {
		setOffsetExpression( (SqmExpression<? extends Number>) offset );
		return this;
	}

	@Override
	public @Nullable JpaExpression<? extends Number> getFetch() {
		return getFetchExpression();
	}

	@Override
	public SqmQueryPart<T> setFetch(@Nullable JpaExpression<? extends Number> fetch) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch );
		return this;
	}

	@Override
	public JpaQueryPart<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch, fetchClauseType );
		return this;
	}

	public abstract void validateQueryStructureAndFetchOwners();

	public void appendHqlString(StringBuilder hql, SqmRenderContext context) {
		final List<SqmSortSpecification> sortSpecifications = getSortSpecifications();
		if ( !sortSpecifications.isEmpty() ) {
			hql.append( " order by " );
			sortSpecifications.get( 0 ).appendHqlString( hql, context );
			for ( int i = 1; i < sortSpecifications.size(); i++ ) {
				hql.append( ", " );
				sortSpecifications.get( i ).appendHqlString( hql, context );
			}

			final SqmExpression<? extends Number> offsetExpression = this.offsetExpression;
			if ( offsetExpression != null ) {
				hql.append( " offset " );
				offsetExpression.appendHqlString( hql, context );
				hql.append( " rows " );
			}
			final SqmExpression<? extends Number> fetchExpression = this.fetchExpression;
			if ( fetchExpression != null ) {
				hql.append( " fetch first " );
				fetchExpression.appendHqlString( hql, context );
				hql.append( switch ( fetchClauseType ) {
					case ROWS_ONLY -> " rows only";
					case ROWS_WITH_TIES -> " rows with ties";
					case PERCENT_ONLY -> " percent rows only";
					case PERCENT_WITH_TIES -> " percent rows with ties";
				} );
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		return object instanceof SqmQueryPart<?> that
			&& getClass() == that.getClass()
			&& Objects.equals( orderByClause, that.orderByClause )
			&& Objects.equals( offsetExpression, that.offsetExpression )
			&& Objects.equals( fetchExpression, that.fetchExpression )
			&& fetchClauseType == that.fetchClauseType;
	}

	@Override
	public int hashCode() {
		int result = Objects.hashCode( orderByClause );
		result = 31 * result + Objects.hashCode( offsetExpression );
		result = 31 * result + Objects.hashCode( fetchExpression );
		result = 31 * result + fetchClauseType.hashCode();
		return result;
	}

	@Override
	public boolean isCompatible(Object object) {
		return object instanceof SqmQueryPart<?> that
			&& getClass() == that.getClass()
			&& SqmCacheable.areCompatible( orderByClause, that.orderByClause )
			&& SqmCacheable.areCompatible( offsetExpression, that.offsetExpression )
			&& SqmCacheable.areCompatible( fetchExpression, that.fetchExpression )
			&& fetchClauseType == that.fetchClauseType;
	}

	@Override
	public int cacheHashCode() {
		int result = SqmCacheable.cacheHashCode( orderByClause );
		result = 31 * result + SqmCacheable.cacheHashCode( offsetExpression );
		result = 31 * result + SqmCacheable.cacheHashCode( fetchExpression );
		result = 31 * result + fetchClauseType.hashCode();
		return result;
	}
}
