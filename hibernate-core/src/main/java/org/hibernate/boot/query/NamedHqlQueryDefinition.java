/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;

import jakarta.annotation.Nonnull;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;

/**
 * Boot-time descriptor of a named query define using HQL.
 *
 * @see jakarta.persistence.NamedQuery
 * @see jakarta.persistence.NamedStatement
 * @see org.hibernate.annotations.NamedQuery
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedHqlQueryDefinition<E> extends NamedQueryDefinition<E> {
	@Nonnull
	String getHqlString();

	@Nonnull
	@Override
	NamedSqmQueryMemento<E> resolve(@Nonnull SessionFactoryImplementor factory);

}
