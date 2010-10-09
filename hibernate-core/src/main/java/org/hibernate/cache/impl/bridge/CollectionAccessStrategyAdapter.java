/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically bridging {@link CollectionRegionAccessStrategy} to {@link CacheConcurrencyStrategy}.
 *
 * @author Steve Ebersole
 */
public class CollectionAccessStrategyAdapter implements CollectionRegionAccessStrategy {
	private final CollectionRegion region;
	private final CacheConcurrencyStrategy ccs;
	private final Settings settings;

	public CollectionAccessStrategyAdapter(CollectionRegion region, CacheConcurrencyStrategy ccs, Settings settings) {
		this.region = region;
		this.ccs = ccs;
		this.settings = settings;
	}

	public CollectionRegion getRegion() {
		return region;
	}

	public Object get(Object key, long txTimestamp) throws CacheException {
		return ccs.get( key, txTimestamp );
	}

	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return putFromLoad( key, value, txTimestamp, version, settings.isMinimalPutsEnabled() );
	}

	public boolean putFromLoad(
			Object key, 
			Object value,
			long txTimestamp,
			Object version,
			boolean minimalPutOverride) throws CacheException {
		return ccs.put( key, value, txTimestamp, version, region.getCacheDataDescription().getVersionComparator(), minimalPutOverride );
	}

	public SoftLock lockItem(Object key, Object version) throws CacheException {
		return ccs.lock( key, version );
	}

	public SoftLock lockRegion() throws CacheException {
		// no-op; CCS did not have such a concept
		return null;
	}

	public void unlockItem(Object key, SoftLock lock) throws CacheException {
		ccs.release( key, lock );
	}

	public void unlockRegion(SoftLock lock) throws CacheException {
		// again, CCS did not have such a concept; but a reasonable
		// proximity is to clear the cache after transaction *as long as*
		// the underlying cache is not JTA aware.
		if ( !region.isTransactionAware() ) {
			ccs.clear();
		}
	}

	public void remove(Object key) throws CacheException {
		ccs.evict( key );
	}

	public void removeAll() throws CacheException {
		// again, CCS did not have such a concept; however a reasonable
		// proximity is to clear the cache.  For non-transaction aware
		// caches, we will also do a clear at the end of the transaction
		ccs.clear();
	}

	public void evict(Object key) throws CacheException {
		ccs.remove( key );
	}

	public void evictAll() throws CacheException {
		ccs.clear();
	}

	public void destroy() {
		ccs.destroy();
	}
}
