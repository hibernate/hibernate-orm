/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.Incubating;
import org.hibernate.Session;
import org.hibernate.StatelessSession;

import org.hibernate.query.SelectionQuery;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.specification.internal.SimpleProjectionSpecificationImpl;

/**
 * Allows a {@link SelectionSpecification} to be augmented with the specification of
 * a single projected {@linkplain SingularAttribute attribute} or {@linkplain Path path}.
 * <pre>
 * var specification =
 *         SelectionSpecification.create(Book.class)
 *                 .restrict(Restriction.contains(Book_.title, "hibernate", false))
 *                 .sort(Order.desc(Book_.title));
 * var projection = SimpleProjectionSpecification.create(specification, Book_.isbn);
 * var isbns = projection.createQuery(session).getResultList();
 * </pre>
 * <p>
 * Use of a {@link Path} allows joining to associated entities.
 * <pre>
 * var specification =
 *         SelectionSpecification.create(Book.class)
 *                 .restrict(Restriction.contains(Book_.title, "hibernate", false))
 *                 .sort(Order.desc(Book_.title));
 * var projection =
 *         SimpleProjectionSpecification.create(specification,
 *                 Path.from(Book.class)
 *                     .to(Book_.publisher)
 *                     .to(Publisher_.name));
 * var publisherNames = projection.createQuery(session).getResultList();
 * </pre>
 *
 * @param <T> The result type of the {@link SelectionSpecification}
 * @param <X> The type of the projected path or attribute
 *
 * @since 7.2
 *
 * @apiNote This interface marked {@link Incubating} is considered experimental.
 *          Changes to the API defined here are fully expected in future releases.
 *
 * @author Gavin King
 */
@Incubating
public interface SimpleProjectionSpecification<T,X> extends QuerySpecification<T> {
	/**
	 * Create a new {@code ProjectionSpecification} which augments the given
	 * {@link SelectionSpecification}.
	 */
	static <T,X> SimpleProjectionSpecification<T,X> create(
			SelectionSpecification<T> selectionSpecification,
			Path<T,X> projectedPath) {
		return new SimpleProjectionSpecificationImpl<>( selectionSpecification, projectedPath );
	}

	/**
	 * Create a new {@code ProjectionSpecification} which augments the given
	 * {@link SelectionSpecification}.
	 */
	static <T,X> SimpleProjectionSpecification<T,X> create(
			SelectionSpecification<T> selectionSpecification,
			SingularAttribute<? super T,X> projectedAttribute) {
		return new SimpleProjectionSpecificationImpl<>( selectionSpecification, projectedAttribute );
	}

	@Override
	SelectionQuery<X> createQuery(Session session);

	@Override
	SelectionQuery<X> createQuery(StatelessSession session);

	@Override
	SelectionQuery<X> createQuery(EntityManager entityManager);

	@Override
	CriteriaQuery<X> buildCriteria(CriteriaBuilder builder);

	@Override
	TypedQueryReference<X> reference();

	@Override
	SimpleProjectionSpecification<T,X> validate(CriteriaBuilder builder);
}
