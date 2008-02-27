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
package org.hibernate.cache.jbc2.access;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.access.SoftLock;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Defines the strategy for transactional access to entity or collection data in
 * a pessimistic-locking JBoss Cache using its 2.x APIs.
 * <p>
 * The intent of this class is to encapsulate common code and serve as a
 * delegate for {@link EntityRegionAccessStrategy} and
 * {@link CollectionRegionAccessStrategy} implementations.
 * </p>
 * 
 * @author Brian Stansberry
 */
public class TransactionalAccessDelegate {

    protected final Cache cache;
    protected final Fqn regionFqn;
    protected final BasicRegionAdapter region;

    public TransactionalAccessDelegate(BasicRegionAdapter adapter) {
        this.region = adapter;
        this.cache = adapter.getCacheInstance();
        this.regionFqn = adapter.getRegionFqn();
    }

    public Object get(Object key, long txTimestamp) throws CacheException {
       
        region.ensureRegionRootExists();
        
        return CacheHelper.get(cache, regionFqn, key);
    }

    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
       
        region.ensureRegionRootExists();

        return CacheHelper.putForExternalRead(cache, regionFqn, key, value);
    }

    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
       
        region.ensureRegionRootExists();

        // We ignore minimalPutOverride. JBossCache putForExternalRead is
        // already about as minimal as we can get; it will promptly return
        // if it discovers that the node we want to write to already exists
        return CacheHelper.putForExternalRead(cache, regionFqn, key, value);
    }

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

    public boolean insert(Object key, Object value, Object version) throws CacheException {
       
        region.ensureRegionRootExists();

        CacheHelper.put(cache, regionFqn, key, value);
        return true;
    }

    public boolean afterInsert(Object key, Object value, Object version) throws CacheException {
        return false;
    }

    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {
       
        region.ensureRegionRootExists();

        CacheHelper.put(cache, regionFqn, key, value);
        return true;
    }

    public boolean afterUpdate(Object key, Object value, Object currentVersion, Object previousVersion, SoftLock lock)
            throws CacheException {
        return false;
    }

    public void remove(Object key) throws CacheException {
       
        region.ensureRegionRootExists();

        CacheHelper.remove(cache, regionFqn, key);
    }

    public void removeAll() throws CacheException {
        evictOrRemoveAll();
    }

    public void evict(Object key) throws CacheException {
       
        region.ensureRegionRootExists();
        
        CacheHelper.remove(cache, regionFqn, key);
    }

    public void evictAll() throws CacheException {
        evictOrRemoveAll();
    }
    
    private void evictOrRemoveAll() throws CacheException {
        CacheHelper.removeAll(cache, regionFqn);        
    }
}
