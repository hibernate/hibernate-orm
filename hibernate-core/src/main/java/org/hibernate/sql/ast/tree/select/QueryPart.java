/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.select;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Christian Beikov
 */
public abstract class QueryPart implements SqlAstNode {
	private final boolean isRoot;

	private boolean hasPositionalSortItem;
	private List<SortSpecification> sortSpecifications;

	private Expression offsetClauseExpression;
	private Expression fetchClauseExpression;
	private FetchClauseType fetchClauseType = FetchClauseType.ROWS_ONLY;

	public QueryPart(boolean isRoot) {
		this.isRoot = isRoot;
	}

	protected QueryPart(boolean isRoot, QueryPart original) {
		this.isRoot = isRoot;
		this.hasPositionalSortItem = original.hasPositionalSortItem;
		this.sortSpecifications = original.sortSpecifications;
		this.offsetClauseExpression = original.offsetClauseExpression;
		this.fetchClauseExpression = original.fetchClauseExpression;
		this.fetchClauseType = original.fetchClauseType;
	}

	public abstract QuerySpec getFirstQuerySpec();

	public abstract QuerySpec getLastQuerySpec();

	public abstract void visitQuerySpecs(Consumer<QuerySpec> querySpecConsumer);

	public abstract <T> T queryQuerySpecs(Function<QuerySpec, T> querySpecConsumer);

	/**
	 * Does this QueryPart map to the statement's root query (as
	 * opposed to one of its sub-queries)?
	 */
	public boolean isRoot() {
		return isRoot;
	}

	public boolean hasSortSpecifications() {
		return sortSpecifications != null && !sortSpecifications.isEmpty();
	}

	public boolean hasPositionalSortItem() {
		return hasPositionalSortItem;
	}

	public List<SortSpecification> getSortSpecifications() {
		return sortSpecifications;
	}

	public void visitSortSpecifications(Consumer<SortSpecification> consumer) {
		if ( sortSpecifications != null ) {
			sortSpecifications.forEach( consumer );
		}
	}

	public void addSortSpecification(SortSpecification specification) {
		if ( sortSpecifications == null ) {
			sortSpecifications = new ArrayList<>();
		}
		sortSpecifications.add( specification );

		if ( isRoot ) {
			if ( specification.getSortExpression() instanceof SqmAliasedNodeRef ) {
				hasPositionalSortItem = true;
			}
		}
	}

	public boolean hasOffsetOrFetchClause() {
		return offsetClauseExpression != null || fetchClauseExpression != null;
	}

	public Expression getOffsetClauseExpression() {
		return offsetClauseExpression;
	}

	public void setOffsetClauseExpression(Expression offsetClauseExpression) {
		this.offsetClauseExpression = offsetClauseExpression;
	}

	public Expression getFetchClauseExpression() {
		return fetchClauseExpression;
	}

	public void setFetchClauseExpression(Expression fetchClauseExpression, FetchClauseType fetchClauseType) {
		if ( fetchClauseExpression == null ) {
			this.fetchClauseExpression = null;
			this.fetchClauseType = FetchClauseType.ROWS_ONLY;
		}
		else {
			if ( fetchClauseType == null ) {
				throw new IllegalArgumentException( "Fetch clause may not be null" );
			}
			this.fetchClauseExpression = fetchClauseExpression;
			this.fetchClauseType = fetchClauseType;
		}
	}

	public FetchClauseType getFetchClauseType() {
		return fetchClauseType;
	}

}
