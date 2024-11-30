/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.query.QueryFlushMode;

/**
 * Enumeration extending the {@linkplain jakarta.persistence.FlushModeType JPA flush modes}
 * with flush modes specific to Hibernate, and a "null" mode, {@link #PERSISTENCE_CONTEXT},
 * for use as a default annotation value. Except for the null value, this enumeration is
 * isomorphic to {@link org.hibernate.FlushMode}.
 *
 * @author Carlos Gonzalez-Cadenas
 *
 * @see NamedQuery#flushMode
 * @see NamedNativeQuery#flushMode
 *
 * @deprecated use {@link QueryFlushMode}
 */
@Deprecated(since="7")
public enum FlushModeType {
	/**
	 * Corresponds to {@link org.hibernate.FlushMode#ALWAYS}.
	 */
	ALWAYS,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#AUTO}.
	 */
	AUTO,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#COMMIT}.
	 */
	COMMIT,
	/**
	 * Corresponds to  {@link org.hibernate.FlushMode#MANUAL}.
	 */
	MANUAL,
	/**
	 * Current flush mode of the persistence context at the time the query is executed.
	 */
	PERSISTENCE_CONTEXT
}
