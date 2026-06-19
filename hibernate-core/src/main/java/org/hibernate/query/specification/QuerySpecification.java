/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.specification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityHandler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Reference;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.hibernate.Incubating;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.restriction.Restriction;

/**
 * Commonality for all query specifications which allow iterative,
 * programmatic building of a query. A specification allows
 * {@linkplain #restrict restrictions},
 * {@linkplain SelectionSpecification#restrict sorting}, and
 * {@linkplain SelectionSpecification#fetch fetching} to be added to
 * a predefined HQL or criteria query.
 * <ul>
 * <li>{@link #createQuery(EntityHandler)} obtains an executable
 *     query object associated with the given session.
 * <li>{@link #buildCriteria(CriteriaBuilder)} transforms the
 *     specification to a {@link CommonAbstractCriteria criteria query}.
 * <li>{@link #validate(CriteriaBuilder)} validates the query without
 *     executing it.
 * </ul>
 * <p>
 * This is the abstract base type of {@link SelectionSpecification}
 * and {@link MutationSpecification}.
 *
 * @apiNote Query specifications only support a single root entity.
 *
 * @param <T> The root entity type.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface QuerySpecification<T> {

	/**
	 * Adds a restriction to the query specification.
	 *
	 * @param restriction The restriction predicate to be added.
	 *
	 * @return {@code this} for method chaining.
	 */
	@Nonnull
	QuerySpecification<T> restrict(@Nonnull Restriction<? super T> restriction);

	/**
	 * Finalize the building and create executable query instance.
	 */
	@Nonnull
	CommonQueryContract createQuery(@Nonnull EntityHandler entityHandler);

	/**
	 * Build a {@link CommonAbstractCriteria criteria query}
	 * satisfying this specification, using the given
	 * {@link CriteriaBuilder}.
	 * <p>
	 * If the returned criteria query is mutated, the mutations
	 * will not be not reflected in this specification.
	 *
	 * @return a new criteria query
	 */
	@Nonnull
	CommonAbstractCriteria buildCriteria(@Nonnull CriteriaBuilder builder);

	/**
	 * Validate the query.
	 *
	 * @return {@code this} if everything is fine
	 * @throws RuntimeException if it ain't all good
	 */
	@Nonnull
	QuerySpecification<T> validate(@Nonnull CriteriaBuilder builder);

	/**
	 * Obtain a {@linkplain TypedQueryReference reference}
	 * to this specification which may be passed along to
	 * {@link EntityManager#createQuery(TypedQueryReference)}.
	 */
	@Nonnull
	Reference reference();
}
