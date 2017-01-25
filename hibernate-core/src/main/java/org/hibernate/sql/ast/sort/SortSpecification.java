/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.sort;

import org.hibernate.sql.ast.expression.Expression;
import org.hibernate.sqm.query.order.SqmSortOrder;

/**
 * @author Steve Ebersole
 */
public class SortSpecification {
	private final Expression sortExpression;
	private final String collation;
	private final SqmSortOrder sortOrder;

	public SortSpecification(Expression sortExpression, String collation, SqmSortOrder sortOrder) {
		this.sortExpression = sortExpression;
		this.collation = collation;
		this.sortOrder = sortOrder;
	}

	public Expression getSortExpression() {
		return sortExpression;
	}

	public String getCollation() {
		return collation;
	}

	public SqmSortOrder getSortOrder() {
		return sortOrder;
	}
}
