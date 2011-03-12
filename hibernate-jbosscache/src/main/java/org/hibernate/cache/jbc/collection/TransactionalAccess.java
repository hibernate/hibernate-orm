/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
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
 */
package org.hibernate.cache.jbc.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc.access.TransactionalAccessDelegate;

/**
 * This defines the strategy for transactional access to collection data in a
 * pessimistic-locking JBossCache using its 2.x APIs
 * 
 * @author Steve Ebersole
 * @author Brian Stansberry
 */
public class TransactionalAccess implements CollectionRegionAccessStrategy {

    private final CollectionRegionImpl region;

    /**
     * Most of our logic is shared between this and entity regions, so we
     * delegate to a class that encapsulates it
     */
    private final TransactionalAccessDelegate delegate;

    /**
     * Create a new TransactionalAccess.
     * 
     * @param region the region to which this provides access
     */
    public TransactionalAccess(CollectionRegionImpl region) {
        this(region, new TransactionalAccessDelegate(region, region.getPutFromLoadValidator()));
    }

    /**
     * Allow subclasses to define the delegate.
     *
     * @param region the region to which this provides access
     * @param delegate type of transactional access
     */
    protected TransactionalAccess(CollectionRegionImpl region, TransactionalAccessDelegate delegate) {
        this.region = region;
        this.delegate = delegate;
    }

    /**
	 * {@inheritDoc}
	 */
	public CollectionRegion getRegion() {
        return region;
    }

    /**
	 * {@inheritDoc}
	 */
	public Object get(Object key, long txTimestamp) throws CacheException {

        return delegate.get(key, txTimestamp);
    }

    /**
	 * {@inheritDoc}
	 */
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {

        return delegate.putFromLoad(key, value, txTimestamp, version);
    }

    /**
	 * {@inheritDoc}
	 */
	public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {

        return delegate.putFromLoad(key, value, txTimestamp, version, minimalPutOverride);
    }

    /**
	 * {@inheritDoc}
	 */
	public void remove(Object key) throws CacheException {

        delegate.remove(key);
    }

    /**
	 * {@inheritDoc}
	 */
	public void removeAll() throws CacheException {
        delegate.removeAll();
    }

    /**
	 * {@inheritDoc}
	 */
	public void evict(Object key) throws CacheException {
        delegate.evict(key);
    }

    /**
	 * {@inheritDoc}
	 */
	public void evictAll() throws CacheException {
        delegate.evictAll();
    }

    /**
	 * {@inheritDoc}
	 */
	public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
	public SoftLock lockRegion() throws CacheException {
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
	public void unlockItem(Object key, SoftLock lock) throws CacheException {
    }

    /**
	 * {@inheritDoc}
	 */
	public void unlockRegion(SoftLock lock) throws CacheException {
    }
}
