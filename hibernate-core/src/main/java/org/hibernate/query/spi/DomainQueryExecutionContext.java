/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.sql.exec.spi.Callback;

/**
 * Context for execution of {@link Query}"
 */
public interface DomainQueryExecutionContext {
	/**
	 * The options to use for execution of the query
	 */
	QueryOptions getQueryOptions();

	/**
	 * The domain parameter bindings
	 */
	QueryParameterBindings getQueryParameterBindings();

	/**
	 * The callback reference
	 */
	Callback getCallback();

	default boolean hasCallbackActions() {
		final Callback callback = getCallback();
		return callback != null && callback.hasAfterLoadActions();
	}

	/**
	 * The underlying session
	 */
	SharedSessionContractImplementor getSession();

	default Class<?> getResultType() {
		return null;
	}
}
