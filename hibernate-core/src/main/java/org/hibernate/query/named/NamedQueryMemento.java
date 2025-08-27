/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.spi.QueryParameterImplementor;

import jakarta.persistence.TypedQueryReference;

/**
 * The runtime representation of named queries.  They are stored in and
 * available through the QueryEngine's {@link NamedObjectRepository}.
 *
 * This is the base contract for all specific types of named query mementos
 *
 * @author Steve Ebersole
 */
public interface NamedQueryMemento<E> extends TypedQueryReference<E> {
	/**
	 * The name under which the query is registered
	 */
	String getRegistrationName();

	@Override
	default String getName() {
		return getRegistrationName();
	}

	Boolean getCacheable();

	String getCacheRegion();

	CacheMode getCacheMode();

	FlushMode getFlushMode();

	Boolean getReadOnly();

	Integer getTimeout();

	Integer getFetchSize();

	String getComment();

	Map<String, Object> getHints();

	void validate(QueryEngine queryEngine);

	/**
	 * Makes a copy of the memento using the specified registration name
	 */
	NamedQueryMemento<E> makeCopy(String name);

	QueryImplementor<E> toQuery(SharedSessionContractImplementor session);
	<T> QueryImplementor<T> toQuery(SharedSessionContractImplementor session, Class<T> javaType);

	interface ParameterMemento {
		QueryParameterImplementor<?> resolve(SharedSessionContractImplementor session);
	}
}
