/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.caching;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface QueryCachePutManager {
	void registerJdbcRow(Object values);

	/**
	 * @deprecated Use {@link #finishUp(int, SharedSessionContractImplementor)} instead
	 */
	@Deprecated(forRemoval = true, since = "6.6")
	void finishUp(SharedSessionContractImplementor session);

	/**
	 * @since 6.6
	 */
	default void finishUp(int resultCount, SharedSessionContractImplementor session) {
		finishUp( session );
	}
}
