/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.CacheMode;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryProducerImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;

import static org.hibernate.internal.util.NullnessHelper.coalesceSuppliedValues;

/**
 * @author Steve Ebersole
 */
public class JdbcExecHelper {
	/**
	 * Singleton access
	 */
	public static final JdbcExecHelper INSTANCE = new JdbcExecHelper();

	private JdbcExecHelper() {
	}

	public static CacheMode resolveCacheMode(ExecutionContext executionContext) {
		return resolveCacheMode( executionContext.getQueryOptions(), executionContext.getSession() );
	}

	public static CacheMode resolveCacheMode(QueryOptions options, QueryProducerImplementor session) {
		return coalesceSuppliedValues(
				() -> options == null ? null : options.getCacheMode(),
				session::getCacheMode,
				() -> CacheMode.NORMAL
		);
	}

	public static CacheMode resolveCacheMode(CacheMode override, QueryProducerImplementor session) {
		return coalesceSuppliedValues(
				() -> override,
				session::getCacheMode,
				() -> CacheMode.NORMAL
		);
	}

}
