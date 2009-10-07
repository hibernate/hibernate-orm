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
package org.hibernate.cache.jbc.entity;

import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc.access.TransactionalAccessDelegate;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.CacheException;

/**
 * Defines the strategy for transactional access to entity data in a
 * pessimistic-locking JBossCache using its 2.x APIs
 * 
 * @author Steve Ebersole
 */
public class TransactionalAccess implements EntityRegionAccessStrategy {

    protected final EntityRegionImpl region;

    /**
     * Most of our logic is shared between this and entity regions, so we
     * delegate to a class that encapsulates it
     */
    private final TransactionalAccessDelegate delegate;

    public TransactionalAccess(EntityRegionImpl region) {
        this(region, new TransactionalAccessDelegate(region, region.getPutFromLoadValidator()));
    }

    protected TransactionalAccess(EntityRegionImpl region, TransactionalAccessDelegate delegate) {
        this.region = region;
        this.delegate = delegate;
    }

    public EntityRegion getRegion() {
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

    public boolean insert(Object key, Object value, Object version) throws CacheException {

        return delegate.insert(key, value, version);
    }

    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {

        return delegate.update(key, value, currentVersion, previousVersion);
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

    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        return false;
    }
}
