package org.hibernate.cache.infinispan.entity;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.infinispan.access.PutFromLoadValidator;
import org.hibernate.cache.infinispan.impl.BaseTransactionalDataRegion;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;

import org.infinispan.AdvancedCache;

/**
 * @author Chris Bredesen
 * @author Galder Zamarreño
 * @since 3.5
 */
public class EntityRegionImpl extends BaseTransactionalDataRegion implements EntityRegion {

	public EntityRegionImpl(AdvancedCache cache, String name,
							CacheDataDescription metadata, RegionFactory factory) {
		super( cache, name, metadata, factory );
	}

	@Override
	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		switch ( accessType ) {
			case READ_ONLY:
				return new ReadOnlyAccess( this );
			case TRANSACTIONAL:
				return new TransactionalAccess( this );
			default:
				throw new CacheException( "Unsupported access type [" + accessType.getExternalName() + "]" );
		}
	}

	public PutFromLoadValidator getPutFromLoadValidator() {
		return new PutFromLoadValidator( cache );
	}

}