/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.caching.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.results.caching.QueryCachePutManager;

/**
 * QueryCachePutManager implementation for cases where we will not be putting
 * Query results into the cache.
 *
 * @author Steve Ebersole
 */
public class QueryCachePutManagerDisabledImpl implements QueryCachePutManager {
	/**
	 * Singleton access
	 */
	public static final QueryCachePutManagerDisabledImpl INSTANCE = new QueryCachePutManagerDisabledImpl();

	private QueryCachePutManagerDisabledImpl() {
	}

	@Override
	public void registerJdbcRow(Object[] values) {
	}

	@Override
	public void finishUp(SharedSessionContractImplementor session) {

	}
}
