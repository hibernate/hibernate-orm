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

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.hibernate.cache.jbc2.util.DataVersionAdapter;
import org.hibernate.cache.jbc2.util.NonLockingDataVersion;
import org.jboss.cache.config.Option;
import org.jboss.cache.optimistic.DataVersion;

/**
 * Defines the strategy for transactional access to entity or collection data in
 * an optimistic-locking JBoss Cache using its 2.x APIs.
 * <p>
 * The intent of this class is to encapsulate common code and serve as a
 * delegate for {@link EntityRegionAccessStrategy} and
 * {@link CollectionRegionAccessStrategy} implementations.
 * </p>
 * 
 * @author Brian Stansberry
 * @version $Revision: 1 $
 */
public class OptimisticTransactionalAccessDelegate extends TransactionalAccessDelegate {

    protected final CacheDataDescription dataDescription;

    public OptimisticTransactionalAccessDelegate(TransactionalDataRegionAdapter region) {
        super(region);
        this.dataDescription = region.getCacheDataDescription();
    }

    /**
     * Overrides the
     * {@link TransactionalAccessDelegate#evict(Object) superclass} by adding a
     * {@link NonLockingDataVersion} to the invocation.
     */
    @Override
    public void evict(Object key) throws CacheException {
        
        region.ensureRegionRootExists();

        Option opt = NonLockingDataVersion.getInvocationOption();
        CacheHelper.remove(cache, regionFqn, key, opt);
    }

    /**
     * Overrides the {@link TransactionalAccessDelegate#evictAll() superclass}
     * by adding a {@link NonLockingDataVersion} to the invocation.
     */
    @Override
    public void evictAll() throws CacheException {

        evictOrRemoveAll();
    }    
    
    /**
     * Overrides the {@link TransactionalAccessDelegate#get(Object, long) superclass}
     * by {@link BasicRegionAdapter#ensureRegionRootExists() ensuring the root
     * node for the region exists} before making the call.
     */
    @Override
    public Object get(Object key, long txTimestamp) throws CacheException
    {
        region.ensureRegionRootExists();
        
        return CacheHelper.get(cache, regionFqn, key);
    }

    /**
     * Overrides the
     * {@link TransactionalAccessDelegate#insert(Object, Object, Object) superclass}
     * by adding a {@link DataVersion} to the invocation.
     */
    @Override
    public boolean insert(Object key, Object value, Object version) throws CacheException {
        
        region.ensureRegionRootExists();

        Option opt = getDataVersionOption(version, null);
        CacheHelper.put(cache, regionFqn, key, value, opt);
        return true;
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
        
        region.ensureRegionRootExists();

        // We ignore minimalPutOverride. JBossCache putForExternalRead is
        // already about as minimal as we can get; it will promptly return
        // if it discovers that the node we want to write to already exists
        Option opt = getDataVersionOption(version, version);
        return CacheHelper.putForExternalRead(cache, regionFqn, key, value, opt);
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
        
        region.ensureRegionRootExists();

        Option opt = getDataVersionOption(version, version);
        return CacheHelper.putForExternalRead(cache, regionFqn, key, value, opt);
    }

    @Override
    public void remove(Object key) throws CacheException {
        
        region.ensureRegionRootExists();

        Option opt = NonLockingDataVersion.getInvocationOption();
        CacheHelper.remove(cache, regionFqn, key, opt);
    }

    @Override
    public void removeAll() throws CacheException {

        evictOrRemoveAll();  
    }

    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {
        
        region.ensureRegionRootExists();

        Option opt = getDataVersionOption(currentVersion, previousVersion);
        CacheHelper.put(cache, regionFqn, key, value, opt);
        return true;
    }

    private Option getDataVersionOption(Object currentVersion, Object previousVersion) {
        
        DataVersion dv = (dataDescription != null && dataDescription.isVersioned()) ? new DataVersionAdapter(
                currentVersion, previousVersion, dataDescription.getVersionComparator(), dataDescription.toString())
                : NonLockingDataVersion.INSTANCE;
        Option opt = new Option();
        opt.setDataVersion(dv);
        return opt;
    }

    private void evictOrRemoveAll() {
       
        Option opt = NonLockingDataVersion.getInvocationOption();
        CacheHelper.removeAll(cache, regionFqn, opt);
    }

}
