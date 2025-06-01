/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import jakarta.persistence.criteria.Nulls;
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
	private Nulls nullPrecedence = Nulls.NONE;
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

	public Nulls getNullPrecedence() {
		return nullPrecedence;
	}

	public void setNullPrecedence(Nulls nullPrecedence) {
		this.nullPrecedence = nullPrecedence;
	}

	public String getOrderByValue() {
		return orderByValue;
	}

	public void setOrderByValue(String orderByValue) {
		this.orderByValue = orderByValue;
	}
}
