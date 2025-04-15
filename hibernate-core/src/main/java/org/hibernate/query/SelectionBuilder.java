/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

/**
 * A builder for a {@link SelectionQuery}.
 *
 * @since 7.0
 */
@Incubating
public interface SelectionBuilder<T> {

	/**
	 * If the result type of this query is an entity class, add one or more
	 * {@linkplain org.hibernate.query.Order rules} for ordering the query results.
	 *
	 * @param orderList one or more instances of {@link org.hibernate.query.Order}
	 *
	 * @see org.hibernate.query.Order
	 *
	 * @since 7.0
	 */
	@Incubating
	SelectionBuilder<T> setOrder(List<? extends Order<? super T>> orderList);

	/**
	 * If the result type of this query is an entity class, add a
	 * {@linkplain org.hibernate.query.Order rule} for ordering the query results.
	 *
	 * @param order an instance of {@link org.hibernate.query.Order}
	 *
	 * @see org.hibernate.query.Order
	 *
	 * @since 7.0
	 */
	@Incubating
	SelectionBuilder<T> setOrder(org.hibernate.query.Order<? super T> order);

	/**
	 * If the result type of this query is an entity class, add a
	 * {@linkplain Restriction rule} for restricting the query results.
	 *
	 * @param restriction an instance of {@link Restriction}
	 *
	 * @see Restriction
	 *
	 * @since 7.0
	 */
	@Incubating
	SelectionBuilder<T> addRestriction(Restriction<? super T> restriction);

	/**
	 * Creates a query for this selection builder.
	 *
	 * @since 7.0
	 */
	SelectionQuery<T> createQuery();
}
