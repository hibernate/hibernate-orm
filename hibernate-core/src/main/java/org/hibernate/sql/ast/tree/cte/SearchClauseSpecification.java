/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.cte;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortOrder;

/**
 * @author Christian Beikov
 */
public class SearchClauseSpecification {
	private final CteColumn cteColumn;
	private final SortOrder sortOrder;
	private final NullPrecedence nullPrecedence;

	public SearchClauseSpecification(CteColumn cteColumn, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		this.cteColumn = cteColumn;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	public CteColumn getCteColumn() {
		return cteColumn;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}
}
