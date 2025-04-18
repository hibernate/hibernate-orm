/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.Root;
import org.hibernate.Incubating;
import org.hibernate.SharedSessionContract;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.restriction.Restriction;

/**
 * Commonality for all query specifications which allow iterative,
 * programmatic building of a query.
 *
 * @apiNote Query specifications only support a {@linkplain #getRoot() single root}.
 *
 * @author Steve Ebersole
 *
 * @since 7.0
 */
@Incubating
public interface QuerySpecification<T> {
	/**
	 * Get the root of the query.
	 * E.g. given the HQL {@code "from Book"}, we have a single {@code Root<Book>}.
	 */
	Root<T> getRoot();

	/**
	 * Access to the criteria query which QuerySpecification is
	 * managing and manipulating internally.
	 * While it is allowable to directly mutate this tree, users
	 * should instead prefer to manipulate the tree through the
	 * methods exposed on the specification itself.
	 */
	CommonAbstractCriteria getCriteria();

	/**
	 * Adds a restriction to the query specification.
	 *
	 * @param restriction The restriction predicate to be added.
	 *
	 * @return {@code this} for method chaining.
	 */
	QuerySpecification<T> addRestriction(Restriction<T> restriction);

	/**
	 * Finalize the building and create executable query instance.
	 */
	CommonQueryContract createQuery(SharedSessionContract session);
}
