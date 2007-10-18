/*
 * Copyright (c) 2007, Red Hat Middleware, LLC. All rights reserved.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, v. 2.1. This program is distributed in the
 * hope that it will be useful, but WITHOUT A WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License, v.2.1 along with this
 * distribution; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2.collection;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc2.access.TransactionalAccessDelegate;

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
     * @param region
     */
    public TransactionalAccess(CollectionRegionImpl region) {
        this(region, new TransactionalAccessDelegate(region.getCacheInstance(), region.getRegionFqn()));
    }

    /**
     * Allow subclasses to define the delegate.
     * 
     * @param region
     * @param delegate
     */
    protected TransactionalAccess(CollectionRegionImpl region, TransactionalAccessDelegate delegate) {
        this.region = region;
        this.delegate = delegate;
    }

    public CollectionRegion getRegion() {
        return region;
    }

    public Object get(Object key, long txTimestamp) throws CacheException {

        return delegate.get(key, txTimestamp);
    }

    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {

        return delegate.putFromLoad(key, value, txTimestamp, version);
    }

    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {

        return delegate.putFromLoad(key, value, txTimestamp, version, minimalPutOverride);
    }

    public void remove(Object key) throws CacheException {

        delegate.remove(key);
    }

    public void removeAll() throws CacheException {
        delegate.removeAll();
    }

    public void evict(Object key) throws CacheException {
        delegate.evict(key);
    }

    public void evictAll() throws CacheException {
        delegate.evictAll();
    }

    // Following methods we don't delegate since they have so little logic
    // it's clearer to just implement them here

    public SoftLock lockItem(Object key, Object version) throws CacheException {
        return null;
    }

    public SoftLock lockRegion() throws CacheException {
        return null;
    }

    public void unlockItem(Object key, SoftLock lock) throws CacheException {
    }

    public void unlockRegion(SoftLock lock) throws CacheException {
    }
}
