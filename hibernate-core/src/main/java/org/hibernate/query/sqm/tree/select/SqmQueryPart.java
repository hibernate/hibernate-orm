/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import java.util.List;

import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.criteria.JpaExpression;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.criteria.JpaQueryPart;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.SqmCopyContext;
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

	private SqmOrderByClause orderByClause;

	private SqmExpression<? extends Number> offsetExpression;
	private SqmExpression<? extends Number> fetchExpression;
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

	public SqmOrderByClause getOrderByClause() {
		return orderByClause;
	}

	public void setOrderByClause(SqmOrderByClause orderByClause) {
		this.orderByClause = orderByClause;
	}

	public SqmExpression<? extends Number> getFetchExpression() {
		return fetchExpression;
	}

	public SqmExpression<? extends Number> getOffsetExpression() {
		return offsetExpression;
	}

	public void setOffsetExpression(SqmExpression<? extends Number> offsetExpression) {
		if ( offsetExpression != null ) {
			offsetExpression.applyInferableType( nodeBuilder.getIntegerType() );
		}
		this.offsetExpression = offsetExpression;
	}

	public void setFetchExpression(SqmExpression<? extends Number> fetchExpression) {
		setFetchExpression( fetchExpression, FetchClauseType.ROWS_ONLY );
	}

	public void setFetchExpression(SqmExpression<? extends Number> fetchExpression, FetchClauseType fetchClauseType) {
		if ( fetchExpression == null ) {
			this.fetchExpression = null;
			this.fetchClauseType = null;
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
		return getOrderByClause() == null ? emptyList() : getOrderByClause().getSortSpecifications();
	}

	@Override
	public SqmQueryPart<T> setSortSpecifications(List<? extends JpaOrder> sortSpecifications) {
		if ( getOrderByClause() == null ) {
			setOrderByClause( new SqmOrderByClause() );
		}

		//noinspection unchecked
		getOrderByClause().setSortSpecifications( (List<SqmSortSpecification>) sortSpecifications );

		return this;
	}

	@Override
	public JpaExpression<? extends Number> getOffset() {
		return getOffsetExpression();
	}

	@Override
	public SqmQueryPart<T> setOffset(JpaExpression<? extends Number> offset) {
		setOffsetExpression( (SqmExpression<? extends Number>) offset );
		return this;
	}

	@Override
	public JpaExpression<? extends Number> getFetch() {
		return getFetchExpression();
	}

	@Override
	public SqmQueryPart<T> setFetch(JpaExpression<? extends Number> fetch) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch );
		return this;
	}

	@Override
	public JpaQueryPart<T> setFetch(JpaExpression<? extends Number> fetch, FetchClauseType fetchClauseType) {
		setFetchExpression( (SqmExpression<? extends Number>) fetch, fetchClauseType );
		return this;
	}

	public abstract void validateQueryStructureAndFetchOwners();

	public void appendHqlString(StringBuilder sb) {
		if ( orderByClause == null || orderByClause.getSortSpecifications().isEmpty() ) {
			return;
		}
		sb.append( " order by " );
		final List<SqmSortSpecification> sortSpecifications = orderByClause.getSortSpecifications();
		sortSpecifications.get( 0 ).appendHqlString( sb );
		for ( int i = 1; i < sortSpecifications.size(); i++ ) {
			sb.append( ", " );
			sortSpecifications.get( i ).appendHqlString( sb );
		}

		if ( offsetExpression != null ) {
			sb.append( " offset " );
			offsetExpression.appendHqlString( sb );
			sb.append( " rows " );
		}
		if ( fetchExpression != null ) {
			sb.append( " fetch first " );
			fetchExpression.appendHqlString( sb );
			switch ( fetchClauseType ) {
				case ROWS_ONLY:
					sb.append( " rows only" );
					break;
				case ROWS_WITH_TIES:
					sb.append( " rows with ties" );
					break;
				case PERCENT_ONLY:
					sb.append( " percent rows only" );
					break;
				case PERCENT_WITH_TIES:
					sb.append( " percent rows with ties" );
					break;
			}
		}
	}
}
