/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionAction;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Base support for decomposing [collection actions][CollectionAction].
///
/// @author Steve Ebersole
public abstract class AbstractCollectionDecomposer implements CollectionDecomposer {
	protected Object lockCacheItem(CollectionAction action, SharedSessionContractImplementor session) {
		if (!action.getPersister().hasCache()) {
			return null;
		}

		final CollectionDataAccess cache = action.getPersister().getCacheAccessStrategy();
		return cache.generateCacheKey(
				action.getKey(),
				action.getPersister(),
				session.getFactory(),
				session.getTenantIdentifier()
		);
		// Note: The actual lock is obtained in CollectionAction.beforeExecutions()
		// We just generate the cache key here for use in post-execution
	}

}
