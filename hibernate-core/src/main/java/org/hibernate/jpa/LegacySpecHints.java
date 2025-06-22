/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

/**
 * Legacy form (the {@code javax.persistence} namespace) of the hints
 * explicitly defined by the Jakarta Persistence.
 *
 * @apiNote These are a temporary migration aids for migrating from
 *          Java Persistence ({@code javax.persistence} namespace)
 *          to Jakarta Persistence ({@code jakarta.persistence} namespace).
 *
 * @see SpecHints
 *
 * @deprecated Use the {@link SpecHints} form instead
 *
 * @author Steve Ebersole
 */
@Deprecated(since = "6.0")
public interface LegacySpecHints {
	/**
	 * @see SpecHints#HINT_SPEC_FETCH_GRAPH
	 */
	String HINT_JAVAEE_FETCH_GRAPH = "javax.persistence.fetchgraph";

	/**
	 * @see SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	String HINT_JAVAEE_LOAD_GRAPH = "javax.persistence.loadgraph";

	/**
	 * @see SpecHints#HINT_SPEC_LOCK_TIMEOUT
	 */
	String HINT_JAVAEE_LOCK_TIMEOUT = "javax.persistence.lock.timeout";

	/**
	 * @see SpecHints#HINT_SPEC_LOCK_SCOPE
	 */
	String HINT_JAVAEE_LOCK_SCOPE = "javax.persistence.lock.scope";

	/**
	 * @see SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	String HINT_JAVAEE_QUERY_TIMEOUT = "javax.persistence.query.timeout";

	/**
	 * @see SpecHints#HINT_SPEC_CACHE_RETRIEVE_MODE
	 */
	String HINT_JAVAEE_CACHE_RETRIEVE_MODE = "javax.persistence.cache.retrieveMode";

	/**
	 * @see SpecHints#HINT_SPEC_CACHE_STORE_MODE
	 */
	String HINT_JAVAEE_CACHE_STORE_MODE = "javax.persistence.cache.storeMode";
}
