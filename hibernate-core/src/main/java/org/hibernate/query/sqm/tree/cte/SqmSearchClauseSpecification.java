/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import org.hibernate.query.SortDirection;
import org.hibernate.query.criteria.JpaCteCriteriaAttribute;
import org.hibernate.query.criteria.JpaSearchOrder;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Christian Beikov
 */
public class SqmSearchClauseSpecification implements JpaSearchOrder {
	private final SqmCteTableColumn cteColumn;
	private final SortDirection sortOrder;
	private NullPrecedence nullPrecedence;

	public SqmSearchClauseSpecification(SqmCteTableColumn cteColumn, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		if ( cteColumn == null ) {
			throw new IllegalArgumentException( "Null cte column" );
		}
		this.cteColumn = cteColumn;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	public SqmSearchClauseSpecification copy(SqmCopyContext context) {
		return new SqmSearchClauseSpecification(
				cteColumn,
				sortOrder,
				nullPrecedence
		);
	}

	public SqmCteTableColumn getCteColumn() {
		return cteColumn;
	}

	@Override
	public JpaSearchOrder nullPrecedence(NullPrecedence precedence) {
		this.nullPrecedence = precedence;
		return this;
	}

	@Override
	public boolean isAscending() {
		return sortOrder == SortDirection.ASCENDING;
	}

	@Override
	public JpaSearchOrder reverse() {
		SortDirection newSortOrder = this.sortOrder == null ? SortDirection.DESCENDING : sortOrder.reverse();
		return new SqmSearchClauseSpecification( cteColumn, newSortOrder, nullPrecedence );
	}

	@Override
	public JpaCteCriteriaAttribute getAttribute() {
		return cteColumn;
	}

	@Override
	public SortDirection getSortOrder() {
		return sortOrder;
	}

	@Override
	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}
}
