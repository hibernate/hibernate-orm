/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
	@Deprecated
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
