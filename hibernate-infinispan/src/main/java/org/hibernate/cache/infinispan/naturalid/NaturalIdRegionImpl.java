package org.hibernate.cache.infinispan.naturalid;

import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.infinispan.util.CacheAdapter;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.NaturalIdRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class NaturalIdRegionImpl extends BaseTransactionalDataRegion implements NaturalIdRegion {
	public NaturalIdRegionImpl(CacheAdapter cacheAdapter,
							   String name, CacheDataDescription metadata,
							   TransactionManager transactionManager, RegionFactory factory) {
		super( cacheAdapter, name, metadata, transactionManager, factory );
	}

	@Override
	public NaturalIdRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		if (AccessType.READ_ONLY.equals(accessType)) {
			return new ReadOnlyAccess(this);
		} else if (AccessType.TRANSACTIONAL.equals(accessType)) {
			return new TransactionalAccess(this);
		}
		throw new CacheException("Unsupported access type [" + accessType.getExternalName() + "]");
	}

	public PutFromLoadValidator getPutFromLoadValidator() {
		return new PutFromLoadValidator(transactionManager);
	}
}
