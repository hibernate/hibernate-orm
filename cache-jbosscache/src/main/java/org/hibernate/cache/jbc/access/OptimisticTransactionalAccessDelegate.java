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
package org.hibernate.cache.jbc.access;

import javax.transaction.Transaction;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cache.jbc.TransactionalDataRegionAdapter;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.cache.jbc.util.DataVersionAdapter;
import org.hibernate.cache.jbc.util.NonLockingDataVersion;
import org.jboss.cache.config.Option;

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

    public OptimisticTransactionalAccessDelegate(TransactionalDataRegionAdapter region, PutFromLoadValidator validator) {
        super(region, validator);
        this.dataDescription = region.getCacheDataDescription();
    }

    /**
     * Overrides the
     * {@link TransactionalAccessDelegate#evict(Object) superclass} by adding a
     * {@link NonLockingDataVersion} to the invocation.
     */
    @Override
    public void evict(Object key) throws CacheException {
    	if (!putValidator.invalidateKey(key)) {
    		throw new CacheException("Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName());
    	}
        region.ensureRegionRootExists();

        Option opt = NonLockingDataVersion.getInvocationOption();
        CacheHelper.remove(cache, regionFqn, key, opt);
    } 
    
    

    @Override
    public void evictAll() throws CacheException
    {
    	if (!putValidator.invalidateRegion()) {
     	   throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
        }        
       
       Transaction tx = region.suspend();
       try {        
          region.ensureRegionRootExists();
          Option opt = NonLockingDataVersion.getInvocationOption();
          CacheHelper.sendEvictAllNotification(cache, regionFqn, region.getMemberId(), opt);
       }
       finally {
          region.resume(tx);
       }
    }

   /**
     * Overrides the
     * {@link TransactionalAccessDelegate#insert(Object, Object, Object) superclass}
     * by adding a {@link DataVersion} to the invocation.
     */
    @Override
    public boolean insert(Object key, Object value, Object version) throws CacheException {
       
        if (!region.checkValid())
            return false;
        
        region.ensureRegionRootExists();

        Option opt = getDataVersionOption(version, null);
        if (this.invalidation) {
        	opt.setCacheModeLocal(true);
        }
        CacheHelper.put(cache, regionFqn, key, value, opt);
        return true;
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version, boolean minimalPutOverride)
            throws CacheException {
       
        if (!region.checkValid())
            return false;
        
        if (!putValidator.acquirePutFromLoadLock(key))
           return false;
        
        try {
	        region.ensureRegionRootExists();
	
	        // We ignore minimalPutOverride. JBossCache putForExternalRead is
	        // already about as minimal as we can get; it will promptly return
	        // if it discovers that the node we want to write to already exists
	        Option opt = getDataVersionOption(version, version);
	        return CacheHelper.putForExternalRead(cache, regionFqn, key, value, opt);
        }
        finally {
        	putValidator.releasePutFromLoadLock(key);
        }
    }

    @Override
    public boolean putFromLoad(Object key, Object value, long txTimestamp, Object version) throws CacheException {
       
        if (!region.checkValid())
            return false;
        
        if (!putValidator.acquirePutFromLoadLock(key))
           return false;
        
        try {
	        region.ensureRegionRootExists();
	
	        Option opt = getDataVersionOption(version, version);
	        return CacheHelper.putForExternalRead(cache, regionFqn, key, value, opt);
        }
        finally {
        	putValidator.releasePutFromLoadLock(key);
        }
    }

    @Override
    public void remove(Object key) throws CacheException {
       
    	if (!putValidator.invalidateKey(key)) {
    		throw new CacheException("Failed to invalidate pending putFromLoad calls for key " + key + " from region " + region.getName());
    	}
        
        // We remove whether or not the region is valid. Other nodes
        // may have already restored the region so they need to
        // be informed of the change.
        
        region.ensureRegionRootExists();

        Option opt = NonLockingDataVersion.getInvocationOption();
        CacheHelper.remove(cache, regionFqn, key, opt);
    }

    @Override
    public void removeAll() throws CacheException {
       if (!putValidator.invalidateRegion()) {
    	   throw new CacheException("Failed to invalidate pending putFromLoad calls for region " + region.getName());
       }
       Option opt = NonLockingDataVersion.getInvocationOption();
       CacheHelper.removeAll(cache, regionFqn, opt);
    }

    @Override
    public boolean update(Object key, Object value, Object currentVersion, Object previousVersion)
            throws CacheException {
       
        // We update whether or not the region is valid. Other nodes
        // may have already restored the region so they need to
        // be informed of the change.
        
        region.ensureRegionRootExists();

        Option opt = getDataVersionOption(currentVersion, previousVersion);
        CacheHelper.put(cache, regionFqn, key, value, opt);
        return true;
    }

    @SuppressWarnings("deprecation")
    private Option getDataVersionOption(Object currentVersion, Object previousVersion) {
        
       org.jboss.cache.optimistic.DataVersion dv = (dataDescription != null && dataDescription.isVersioned()) ? new DataVersionAdapter(
                currentVersion, previousVersion, dataDescription.getVersionComparator(), dataDescription.toString())
                : NonLockingDataVersion.INSTANCE;
        Option opt = new Option();
        opt.setDataVersion(dv);
        return opt;
    }

}
