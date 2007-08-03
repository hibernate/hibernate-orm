package org.hibernate.cache.impl.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.OptimisticCache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.TransactionalCache;
import org.hibernate.cache.ReadWriteCache;
import org.hibernate.cache.NonstrictReadWriteCache;
import org.hibernate.cache.ReadOnlyCache;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically bridging {@link CollectionRegion} to {@link Cache}.
 *
 * @author Steve Ebersole
 */
public class CollectionRegionAdapter extends BaseTransactionalDataRegionAdapter implements CollectionRegion {
	private static final Logger log = LoggerFactory.getLogger( CollectionRegionAdapter.class );

	public CollectionRegionAdapter(Cache underlyingCache, Settings settings, CacheDataDescription metadata) {
		super( underlyingCache, settings, metadata );
		if ( underlyingCache instanceof OptimisticCache ) {
			( ( OptimisticCache ) underlyingCache ).setSource( new OptimisticCacheSourceAdapter( metadata ) );
		}
	}

	public CollectionRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		CacheConcurrencyStrategy ccs;
		if ( AccessType.READ_ONLY.equals( accessType ) ) {
			if ( metadata.isMutable() ) {
				log.warn( "read-only cache configured for mutable collection [" + getName() + "]" );
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
		return new CollectionAccessStrategyAdapter( this, ccs, settings );
	}
}
