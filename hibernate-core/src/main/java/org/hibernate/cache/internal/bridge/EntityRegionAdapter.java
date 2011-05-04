/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.internal.bridge;

import org.jboss.logging.Logger;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.Cache;
import org.hibernate.cache.spi.CacheConcurrencyStrategy;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.NonstrictReadWriteCache;
import org.hibernate.cache.spi.OptimisticCache;
import org.hibernate.cache.spi.ReadOnlyCache;
import org.hibernate.cache.spi.ReadWriteCache;
import org.hibernate.cache.spi.TransactionalCache;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Adapter specifically bridging {@link EntityRegion} to {@link org.hibernate.cache.spi.Cache}.
 *
 * @author Steve Ebersole
 */
public class EntityRegionAdapter extends BaseTransactionalDataRegionAdapter implements EntityRegion {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, EntityRegionAdapter.class.getName());

	public EntityRegionAdapter(Cache underlyingCache, Settings settings, CacheDataDescription metadata) {
		super( underlyingCache, settings, metadata );
		if ( underlyingCache instanceof OptimisticCache ) {
			( (OptimisticCache) underlyingCache ).setSource( new OptimisticCacheSourceAdapter( metadata ) );
		}
	}

	public EntityRegionAccessStrategy buildAccessStrategy(AccessType accessType) throws CacheException {
		CacheConcurrencyStrategy ccs;
		if ( AccessType.READ_ONLY.equals( accessType ) ) {
            if (metadata.isMutable()) LOG.readOnlyCacheConfiguredForMutableCollection(getName());
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
