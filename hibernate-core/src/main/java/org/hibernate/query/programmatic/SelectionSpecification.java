/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Incubating;
import org.hibernate.query.Order;
import org.hibernate.query.QueryProducer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Restriction;

import java.util.List;

/**
 * Specialization of QuerySpecification for building
 * {@linkplain SelectionQuery selection queries} supporting ordering
 * in addition to restrictions.
 * Once all {@linkplain #addOrdering sorting} and {@linkplain #addRestriction restrictions}
 * are defined, call {@linkplain #createQuery()} to obtain the executable form.
 *
 * @see QueryProducer#createSelectionSpecification(String, Class)
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SelectionSpecification<T> extends QuerySpecification<T> {
	/**
	 * Adds an ordering to the selection specification.
	 * Appended to any previous ordering.
	 *
	 * @param order The ordering fragment to be added.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> addOrdering(Order<T> order);

	/**
	 * Sets the ordering for this selection specification.
	 * If ordering was already defined, this method drops the previous ordering
	 * in favor of the passed {@code orders}.
	 *
	 * @param order The ordering fragment to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> setOrdering(Order<T> order);

	/**
	 * Sets the sorting for this selection specification.
	 * If sorting was already defined, this method drops the previous sorting
	 * in favor of the passed {@code orders}.
	 *
	 * @param orders The sorting fragments to be used.
	 *
	 * @return {@code this} for method chaining.
	 */
	SelectionSpecification<T> setOrdering(List<Order<T>> orders);

	/**
	 * Covariant override.
	 */
	@Override
	CriteriaQuery<T> getCriteria();

	/**
	 * Covariant override.
	 */
	@Override
	SelectionSpecification<T> addRestriction(Restriction<T> restriction);

	/**
	 * Covariant override.
	 */
	SelectionQuery<T> createQuery();
}
