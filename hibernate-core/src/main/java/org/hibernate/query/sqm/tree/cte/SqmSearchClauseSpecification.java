/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.cte;

import java.io.Serializable;

import org.hibernate.query.sqm.NullPrecedence;
import org.hibernate.query.sqm.SortOrder;

/**
 * @author Christian Beikov
 */
public class SqmSearchClauseSpecification implements Serializable {
	private final SqmCteTableColumn cteColumn;
	private final SortOrder sortOrder;
	private final NullPrecedence nullPrecedence;

	public SqmSearchClauseSpecification(SqmCteTableColumn cteColumn, SortOrder sortOrder, NullPrecedence nullPrecedence) {
		this.cteColumn = cteColumn;
		this.sortOrder = sortOrder;
		this.nullPrecedence = nullPrecedence;
	}

	public SqmCteTableColumn getCteColumn() {
		return cteColumn;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}

	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}
}
