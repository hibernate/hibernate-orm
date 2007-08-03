package org.hibernate.cache.impl.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.OptimisticCache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.ReadOnlyCache;
import org.hibernate.cache.ReadWriteCache;
import org.hibernate.cache.NonstrictReadWriteCache;
import org.hibernate.cache.TransactionalCache;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically bridging {@link EntityRegion} to {@link Cache}.
 *
 * @author Steve Ebersole
 */
public class EntityRegionAdapter extends BaseTransactionalDataRegionAdapter implements EntityRegion {
	private static final Logger log = LoggerFactory.getLogger( EntityRegionAdapter.class );

	public EntityRegionAdapter(Cache underlyingCache, Settings settings, CacheDataDescription metadata) {
		super( underlyingCache, settings, metadata );
		if ( underlyingCache instanceof OptimisticCache ) {
			( ( OptimisticCache ) underlyingCache ).setSource( new OptimisticCacheSourceAdapter( metadata ) );
		}
	}

	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		CacheConcurrencyStrategy ccs;
		if ( AccessType.READ_ONLY.equals( accessType ) ) {
			if ( metadata.isMutable() ) {
				log.warn( "read-only cache configured for mutable entity [" + getName() + "]" );
			}
			ccs = new ReadOnlyCache();
		}
		else if ( AccessType.READ_WRITE.equals( accessType ) ) {
			ccs = new ReadWriteCache();
		}
		else if ( AccessType.NONSTRICT_READ_WRITE.equals( accessType ) ) {
			ccs = new NonstrictReadWriteCache();
		}
		else if ( AccessType.TRANSACTIONAL.equals( accessType ) ) {
			ccs = new TransactionalCache();
		}
		else {
			throw new IllegalArgumentException( "unrecognized access strategy type [" + accessType + "]" );
		}
		ccs.setCache( underlyingCache );
		return new EntityAccessStrategyAdapter( this, ccs, settings );
	}

}
