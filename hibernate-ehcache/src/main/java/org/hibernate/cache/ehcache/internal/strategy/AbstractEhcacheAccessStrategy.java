/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cache.ehcache.internal.strategy;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.internal.regions.EhcacheTransactionalDataRegion;
import org.hibernate.cache.spi.access.SoftLock;
import org.hibernate.cfg.Settings;

/**
 * Ultimate superclass for all Ehcache specific Hibernate AccessStrategy implementations.
 *
 * @param <T> type of the enclosed region
 *
 * @author Chris Dennis
 * @author Alex Snaps
 */
abstract class AbstractEhcacheAccessStrategy<T extends EhcacheTransactionalDataRegion> {
	private final T region;
	private final Settings settings;

	/**
	 * Create an access strategy wrapping the given region.
	 *
	 * @param region The wrapped region.  Accessible to subclasses via {@link #region()}
	 * @param settings The Hibernate settings.  Accessible to subclasses via {@link #settings()}
	 */
	AbstractEhcacheAccessStrategy(T region, Settings settings) {
		this.region = region;
		this.settings = settings;
	}

	/**
	 * The wrapped Hibernate cache region.
	 */
	protected T region() {
		return region;
	}

	/**
	 * The settings for this persistence unit.
	 */
	protected Settings settings() {
		return settings;
	}

	/**
	 * This method is a placeholder for method signatures supplied by interfaces pulled in further down the class
	 * hierarchy.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object)
	 */
	public final boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
		return putFromLoad( key, value, txTimestamp, version, settings.isMinimalPutsEnabled() );
	}

	/**
	 * This method is a placeholder for method signatures supplied by interfaces pulled in further down the class
	 * hierarchy.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#putFromLoad(java.lang.Object, java.lang.Object, long, java.lang.Object, boolean)
	 */
	public abstract boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
			throws CacheException;

	/**
	 * Region locks are not supported.
	 *
	 * @return <code>null</code>
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#lockRegion()
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#lockRegion()
	 */
	@SuppressWarnings("UnusedDeclaration")
	public final SoftLock lockRegion() {
		return null;
	}

	/**
	 * Region locks are not supported - perform a cache clear as a precaution.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#unlockRegion(org.hibernate.cache.spi.access.SoftLock)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#unlockRegion(org.hibernate.cache.spi.access.SoftLock)
	 */
	@SuppressWarnings("UnusedDeclaration")
	public final void unlockRegion(SoftLock lock) throws CacheException {
		region.clear();
	}

	/**
	 * A no-op since this is an asynchronous cache access strategy.
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#remove(java.lang.Object)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#remove(java.lang.Object)
	 */
	public void remove(Object key) throws CacheException {
	}

	/**
	 * Called to evict data from the entire region
	 *
	 * @throws CacheException Propogated from underlying {@link org.hibernate.cache.spi.Region}
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#removeAll()
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#removeAll()
	 */
	@SuppressWarnings("UnusedDeclaration")
	public final void removeAll() throws CacheException {
		region.clear();
	}

	/**
	 * Remove the given mapping without regard to transactional safety
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#evict(java.lang.Object)
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#evict(java.lang.Object)
	 */
	public final void evict(Object key) throws CacheException {
		region.remove( key );
	}

	/**
	 * Remove all mappings without regard to transactional safety
	 *
	 * @see org.hibernate.cache.spi.access.EntityRegionAccessStrategy#evictAll()
	 * @see org.hibernate.cache.spi.access.CollectionRegionAccessStrategy#evictAll()
	 */
	@SuppressWarnings("UnusedDeclaration")
	public final void evictAll() throws CacheException {
		region.clear();
	}
}
