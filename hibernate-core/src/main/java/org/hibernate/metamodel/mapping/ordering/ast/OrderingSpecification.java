/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.query.NullPrecedence;
import org.hibernate.query.SortDirection;

/**
 * An individual sort specification in an order-by fragment
 *
 * @author Steve Ebersole
 */
public class OrderingSpecification implements Node {
	private final OrderingExpression orderingExpression;

	private String collation;
	private SortDirection sortOrder = SortDirection.ASCENDING;
	private NullPrecedence nullPrecedence = NullPrecedence.NONE;
	private String orderByValue;

	public OrderingSpecification(OrderingExpression orderingExpression, String orderByValue) {
		this.orderingExpression = orderingExpression;
		this.orderByValue = orderByValue;
	}

	public OrderingExpression getExpression() {
		return orderingExpression;
	}

	public String getCollation() {
		return collation;
	}

	public void setCollation(String collation) {
		this.collation = collation;
	}

	public SortDirection getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(SortDirection sortOrder) {
		this.sortOrder = sortOrder;
	}

	public NullPrecedence getNullPrecedence() {
		return nullPrecedence;
	}

	public void setNullPrecedence(NullPrecedence nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
	}

	public String getOrderByValue() {
		return orderByValue;
	}

	public void setOrderByValue(String orderByValue) {
		this.orderByValue = orderByValue;
	}
}
