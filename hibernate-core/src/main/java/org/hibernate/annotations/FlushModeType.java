/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.FlushMode;
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
 * @deprecated Use {@link QueryFlushMode}. This enumeration will be removed to alleviate
 *             the duplication in naming with {@link jakarta.persistence.FlushModeType}.
 */
@Deprecated(since="7", forRemoval = true)
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
	PERSISTENCE_CONTEXT;

	public QueryFlushMode toQueryFlushMode() {
		return switch ( this ) {
			case AUTO, ALWAYS -> QueryFlushMode.FLUSH;
			case MANUAL, COMMIT -> QueryFlushMode.NO_FLUSH;
			case PERSISTENCE_CONTEXT -> QueryFlushMode.DEFAULT;
		};
	}

	public FlushMode toFlushMode() {
		return switch ( this ) {
			case AUTO -> FlushMode.AUTO;
			case ALWAYS -> FlushMode.ALWAYS;
			case MANUAL -> FlushMode.MANUAL;
			case COMMIT -> FlushMode.COMMIT;
			case PERSISTENCE_CONTEXT -> null;
		};
	}
}
