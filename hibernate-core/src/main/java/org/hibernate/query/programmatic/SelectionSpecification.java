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
 * Specialization of {@link QuerySpecification} for programmatic customization of
 * {@linkplain SelectionQuery selection queries} with ordering and restriction criteria.
 * <ul>
 * <li>The method {@link #addRestriction(Restriction)} allows application of additional
 *     {@linkplain Restriction filtering} to the query results. The static factory methods
 *     of {@link Restriction} are used to express filtering criteria of various kinds.
 * <li>Refinement or replacement of the query sorting criteria is possible via the methods
 *     {@link #addOrdering(Order)} and {@link #setOrdering(List)}, together with the static
 *     factory methods of {@link Order}.
 * </ul>
 * <p>
 * Once all {@linkplain #addOrdering sorting} and {@linkplain #addRestriction restrictions}
 * are specified, call {@linkplain #createQuery()} to obtain an {@linkplain SelectionQuery
 * executable selection query object}.
 * <pre>
 * session.createSelectionSpecification("from Book", Book.class)
 *         .addRestriction(Restriction.contains(Book_.title, "hibernate", false))
 *         .setOrdering(Order.desc(Book_.title))
 *         .createQuery()                       // obtain a SelectionQuery
 *         .setPage(Page.first(50))
 *         .getResultList();
 * </pre>
 * <p>
 * A {@code SelectionSpecification} always represents a query which returns a singe root
 * entity. The restriction and ordering criteria are interpreted as applying to the field
 * and properties of this root entity.
 *
 * @param <T> The entity type returned by the query
 *
 * @see QueryProducer#createSelectionSpecification(String, Class)
 *
 * @author Steve Ebersole
 *
 * @since 7.0
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
	@Override
	SelectionQuery<T> createQuery();
}
