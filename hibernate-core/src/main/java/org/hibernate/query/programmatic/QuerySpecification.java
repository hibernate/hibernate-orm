/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.programmatic;

import org.hibernate.Incubating;
import org.hibernate.SessionFactory;
import org.hibernate.SharedSessionContract;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.CommonQueryContract;
import org.hibernate.query.restriction.Restriction;

/**
 * Commonality for all query specifications which allow iterative,
 * programmatic building of a query.
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
	QuerySpecification<T> restrict(Restriction<T> restriction);

	/**
	 * Finalize the building and create executable query instance.
	 */
	CommonQueryContract createQuery(SharedSessionContract session);

	/**
	 * Validate the query.
	 */
	default void validate(SessionFactory factory) {
		// Extremely temporary implementation.
		// We don't actually want to open a session here,
		// nor create an instance of CommonQueryContract.
		try ( var session = ((SessionFactoryImplementor) factory).openTemporarySession() ) {
			createQuery( session );
		}
	}
}
