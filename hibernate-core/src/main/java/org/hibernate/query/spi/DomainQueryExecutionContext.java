/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
