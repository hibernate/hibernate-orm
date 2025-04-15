/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import org.hibernate.SharedSessionContract;
import org.hibernate.query.Order;
import org.hibernate.query.SelectionBuilder;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

public class SelectionBuilderImpl<T> implements SelectionBuilder<T> {

	private final JpaCriteriaQuery<T> query;
	private final SharedSessionContract session;

	public SelectionBuilderImpl(JpaCriteriaQuery<T> query, SharedSessionContract session) {
		this.query = query;
		this.session = session;
	}

	@Override
	public SelectionBuilder<T> setOrder(List<? extends Order<? super T>> orderList) {
		query.setOrder( orderList );
		return this;
	}

	@Override
	public SelectionBuilder<T> setOrder(Order<? super T> order) {
		query.setOrder( order );
		return this;
	}

	@Override
	public SelectionBuilder<T> addRestriction(Restriction<? super T> restriction) {
		query.addRestriction( restriction );
		return this;
	}

	@Override
	public SelectionQuery<T> createQuery() {
		return session.createQuery( query );
	}
}
