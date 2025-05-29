/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa;

import java.util.Set;

import org.hibernate.jpa.internal.HintsCollector;

/**
 * List of all supported hints that may be passed to {@link jakarta.persistence.Query#setHint(String, Object)}.
 *
 * @see AvailableHints
 *
 * @deprecated Use {@link AvailableHints} instead
 */
@SuppressWarnings("unused")
@Deprecated(since = "6.0")
public final class QueryHints {
	/**
	 * @see SpecHints#HINT_SPEC_QUERY_TIMEOUT
	 */
	public static final String JAKARTA_SPEC_HINT_TIMEOUT = SpecHints.HINT_SPEC_QUERY_TIMEOUT;

	/**
	 * @see HibernateHints#HINT_COMMENT
	 */
	public static final String HINT_COMMENT = HibernateHints.HINT_COMMENT;

	/**
	 * @see HibernateHints#HINT_FETCH_SIZE
	 */
	public static final String HINT_FETCH_SIZE = HibernateHints.HINT_FETCH_SIZE;

	/**
	 * @see HibernateHints#HINT_CACHEABLE
	 */
	public static final String HINT_CACHEABLE = HibernateHints.HINT_CACHEABLE;

	/**
	 * @see HibernateHints#HINT_CACHE_REGION
	 */
	public static final String HINT_CACHE_REGION = HibernateHints.HINT_CACHE_REGION;

	/**
	 * @see HibernateHints#HINT_CACHE_MODE
	 */
	public static final String HINT_CACHE_MODE = HibernateHints.HINT_CACHE_MODE;

	/**
	 * @see HibernateHints#HINT_READ_ONLY
	 */
	public static final String HINT_READONLY = HibernateHints.HINT_READ_ONLY;

	/**
	 * @see HibernateHints#HINT_FLUSH_MODE
	 */
	public static final String HINT_FLUSH_MODE = HibernateHints.HINT_FLUSH_MODE;

	/**
	 * @see HibernateHints#HINT_NATIVE_LOCK_MODE
	 */
	public static final String HINT_NATIVE_LOCKMODE = HibernateHints.HINT_NATIVE_LOCK_MODE;

	/**
	 * @see SpecHints#HINT_SPEC_FETCH_GRAPH
	 */
	public static final String JAKARTA_HINT_FETCH_GRAPH = SpecHints.HINT_SPEC_FETCH_GRAPH;

	/**
	 * @see SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	public static final String JAKARTA_HINT_FETCHGRAPH = SpecHints.HINT_SPEC_FETCH_GRAPH;

	/**
	 * @see SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	public static final String JAKARTA_HINT_LOAD_GRAPH = SpecHints.HINT_SPEC_LOAD_GRAPH;

	/**
	 * @see SpecHints#HINT_SPEC_LOAD_GRAPH
	 */
	public static final String JAKARTA_HINT_LOADGRAPH = SpecHints.HINT_SPEC_LOAD_GRAPH;

	/**
	 * @see HibernateHints#HINT_FOLLOW_ON_STRATEGY
	 */
	public static final String HINT_FOLLOW_ON_STRATEGY = HibernateHints.HINT_FOLLOW_ON_STRATEGY;

	/**
	 * @see HibernateHints#HINT_FOLLOW_ON_LOCKING
	 * @deprecated Use {@linkplain #HINT_FOLLOW_ON_STRATEGY} instead.
	 */
	@Deprecated(since="7.1")
	public static final String HINT_FOLLOW_ON_LOCKING = HibernateHints.HINT_FOLLOW_ON_LOCKING;

	/**
	 * @see HibernateHints#HINT_NATIVE_SPACES
	 */
	public static final String HINT_NATIVE_SPACES = HibernateHints.HINT_NATIVE_SPACES;

	/**
	 * @see LegacySpecHints#HINT_JAVAEE_FETCH_GRAPH
	 */
	public static final String HINT_FETCHGRAPH = LegacySpecHints.HINT_JAVAEE_FETCH_GRAPH;

	/**
	 * @see LegacySpecHints#HINT_JAVAEE_LOAD_GRAPH
	 */
	public static final String HINT_LOADGRAPH = LegacySpecHints.HINT_JAVAEE_LOAD_GRAPH;

	/**
	 * The hint key for specifying a query timeout per Hibernate O/RM, which defines the timeout in seconds.
	 *
	 * @see HibernateHints#HINT_TIMEOUT
	 */
	public static final String HINT_TIMEOUT = HibernateHints.HINT_TIMEOUT;

	/**
	 * @see LegacySpecHints#HINT_JAVAEE_QUERY_TIMEOUT
	 */
	public static final String SPEC_HINT_TIMEOUT = LegacySpecHints.HINT_JAVAEE_QUERY_TIMEOUT;

	public static Set<String> getDefinedHints() {
		return HintsCollector.getDefinedHints();
	}

	private QueryHints() {
	}
}
