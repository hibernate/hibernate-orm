/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;

/**
 * @author Steve Ebersole
 */
public interface ExecutionContext {
	SharedSessionContractImplementor getSession();

	QueryOptions getQueryOptions();

	default LoadQueryInfluencers getLoadQueryInfluencers() {
		return getSession().getLoadQueryInfluencers();
	}

	QueryParameterBindings getQueryParameterBindings();

	Callback getCallback();

	/**
	 * Get the collection key for the collection which is to be loaded immediately.
	 */
	default CollectionKey getCollectionKey() {
		return null;
	}

	/**
	 * Hook to allow delaying calls to {@link LogicalConnectionImplementor#afterStatement()}.
	 * Mainly used in the case of batching and multi-table mutations
	 */
	default void afterStatement(LogicalConnectionImplementor logicalConnection) {
		logicalConnection.afterStatement();
	}
}
