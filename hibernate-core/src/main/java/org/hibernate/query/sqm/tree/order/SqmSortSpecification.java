/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.order;

import org.hibernate.SortOrder;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmSortSpecification {
	private final SqmExpression sortExpression;
	private final String collation;
	private final SortOrder sortOrder;

	public SqmSortSpecification(SqmExpression sortExpression, String collation, SortOrder sortOrder) {
		this.sortExpression = sortExpression;
		this.collation = collation;
		this.sortOrder = sortOrder;
	}

	public SqmSortSpecification(SqmExpression sortExpression) {
		this( sortExpression, null, null );
	}

	public SqmSortSpecification(SqmExpression sortExpression, SortOrder sortOrder) {
		this( sortExpression, null, sortOrder );
	}

	public SqmExpression getSortExpression() {
		return sortExpression;
	}

	public String getCollation() {
		return collation;
	}

	public SortOrder getSortOrder() {
		return sortOrder;
	}
}
