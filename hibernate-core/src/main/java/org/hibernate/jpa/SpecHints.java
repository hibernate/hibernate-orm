/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import java.util.Map;

import org.hibernate.graph.GraphSemantic;

import jakarta.persistence.LockModeType;

/**
 * The hints explicitly defined by the Jakarta Persistence specification
 * which are available for both queries and loading.
 *
 * @see jakarta.persistence.EntityManager#setProperty
 * @see jakarta.persistence.EntityManager#find(Class, Object, Map)
 * @see jakarta.persistence.EntityManager#find(Class, Object, LockModeType, Map)
 * @see jakarta.persistence.EntityManager#lock(Object, LockModeType, Map)
 * @see jakarta.persistence.Query#setHint
 *
 * @author Steve Ebersole
 */
public interface SpecHints {
	/**
	 * Hint providing a {@link jakarta.persistence.EntityGraph} which should be
	 * interpreted as a "fetch graph".
	 *
	 * @see GraphSemantic#FETCH
	 */
	String HINT_SPEC_FETCH_GRAPH = "jakarta.persistence.fetchgraph";

	/**
	 * Hint providing a {@link jakarta.persistence.EntityGraph} which should be
	 * interpreted as a "load graph".
	 *
	 * @see GraphSemantic#LOAD
	 */
	String HINT_SPEC_LOAD_GRAPH = "jakarta.persistence.loadgraph";

	/**
	 * Hint requesting a pessimistic lock timeout (in milliseconds).
	 */
	String HINT_SPEC_LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";

	/**
	 * Hint indicating whether to extend pessimistic locking to
	 * associated tables.  Expected to be an instance of
	 * {@link jakarta.persistence.PessimisticLockScope}
	 */
	String HINT_SPEC_LOCK_SCOPE = "jakarta.persistence.lock.scope";

	/**
	 * The Jakarta Persistence defined hint for requesting a timeout
	 * be applied to a {@link jakarta.persistence.Query} executions.
	 *
	 * @implSpec Not valid for load and/or lock operations
	 */
	String HINT_SPEC_QUERY_TIMEOUT = "jakarta.persistence.query.timeout";

	/**
	 * Hint specifying how Hibernate should interact with the second level cache
	 * when it has the opportunity to retrieve data from the cache. The value must
	 * be a {@link jakarta.persistence.CacheRetrieveMode}.
	 *
	 * @see jakarta.persistence.CacheRetrieveMode
	 * @see jakarta.persistence.EntityManager#setProperty
	 * @see jakarta.persistence.Query#setHint
	 * @see org.hibernate.CacheMode
	 */
	String HINT_SPEC_CACHE_RETRIEVE_MODE = "jakarta.persistence.cache.retrieveMode";

	/**
	 * Hint specifying how Hibernate should interact with the second level cache
	 * when it has the opportunity to store data in the cache. The value must be a
	 * {@link jakarta.persistence.CacheStoreMode}.
	 *
	 * @see jakarta.persistence.CacheStoreMode
	 * @see jakarta.persistence.EntityManager#setProperty
	 * @see jakarta.persistence.Query#setHint
	 * @see org.hibernate.CacheMode
	 */
	String HINT_SPEC_CACHE_STORE_MODE = "jakarta.persistence.cache.storeMode";

}
