package org.hibernate.sql.results.caching.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface QueryCachePutManager {
	void registerJdbcRow(Object values);

	/**
	 * @since 6.6
	 */
	void finishUp(int resultCount, SharedSessionContractImplementor session);
}
