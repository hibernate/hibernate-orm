/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.spi.NamedNativeQueryMemento;

import java.util.Set;

/**
 * Boot-time descriptor of a named native query.
 *
 * @see org.hibernate.annotations.NamedNativeQuery
 * @see jakarta.persistence.NamedNativeQuery
 * @see jakarta.persistence.NamedNativeStatement
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface NamedNativeQueryDefinition<E> extends NamedQueryDefinition<E> {
	@Nonnull
	String getSqlQueryString();

	@Nullable
	String getResultSetMappingName();

	@Nullable
	Set<String> getQuerySpaces();

	@Nonnull
	@Override
	NamedNativeQueryMemento<E> resolve(@Nonnull SessionFactoryImplementor factory);

}
