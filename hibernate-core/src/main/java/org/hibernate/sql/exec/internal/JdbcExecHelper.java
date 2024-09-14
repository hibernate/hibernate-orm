/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.internal;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
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

	public static CacheMode resolveCacheMode(QueryOptions options, SharedSessionContractImplementor session) {
		return coalesceSuppliedValues(
				() -> options == null ? null : options.getCacheMode(),
				session::getCacheMode,
				() -> CacheMode.NORMAL
		);
	}

	public static CacheMode resolveCacheMode(CacheMode override, SharedSessionContractImplementor session) {
		return coalesceSuppliedValues(
				() -> override,
				session::getCacheMode,
				() -> CacheMode.NORMAL
		);
	}

}
