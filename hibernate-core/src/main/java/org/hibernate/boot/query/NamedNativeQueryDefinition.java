/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.query;


import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.named.NamedNativeQueryMemento;

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
	String getSqlQueryString();

	String getResultSetMappingName();

	Set<String> getQuerySpaces();

	@Override
	NamedNativeQueryMemento<E> resolve(SessionFactoryImplementor factory);

}
