package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.TransactionalDataRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cfg.Settings;

/**
 * {@inheritDoc}
*
* @author Steve Ebersole
*/
public abstract class BaseTransactionalDataRegionAdapter
		extends BaseRegionAdapter
		implements TransactionalDataRegion {

	protected final CacheDataDescription metadata;

	protected BaseTransactionalDataRegionAdapter(Cache underlyingCache, Settings settings, CacheDataDescription metadata) {
		super( underlyingCache, settings );
		this.metadata = metadata;
	}

	public boolean isTransactionAware() {
		return underlyingCache instanceof org.hibernate.cache.TransactionAwareCache;
	}

	public CacheDataDescription getCacheDataDescription() {
		return metadata;
	}
}
