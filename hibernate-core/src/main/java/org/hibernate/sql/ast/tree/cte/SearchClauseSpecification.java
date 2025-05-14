/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;

import jakarta.persistence.criteria.Nulls;

/**
 * @author Christian Beikov
 */
public class SearchClauseSpecification {
	private final CteColumn cteColumn;
	private final SortDirection sortOrder;
	private final Nulls nullPrecedence;

	public SearchClauseSpecification(CteColumn cteColumn, SortDirection sortOrder, Nulls nullPrecedence) {
		this.cteColumn = cteColumn;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	/**
	 * @deprecated Use {@link SearchClauseSpecification#SearchClauseSpecification(CteColumn,SortDirection,Nulls)} instead
	 */
	@Deprecated(since = "7", forRemoval = true)
	public SearchClauseSpecification(CteColumn cteColumn, SortDirection sortOrder, NullPrecedence nullPrecedence) {
		this( cteColumn, sortOrder, nullPrecedence.getJpaValue() );
	}

	public CteColumn getCteColumn() {
		return cteColumn;
	}

	public SortDirection getSortOrder() {
		return sortOrder;
	}

	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}
}
