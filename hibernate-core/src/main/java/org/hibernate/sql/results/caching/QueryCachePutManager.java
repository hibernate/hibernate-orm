/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
