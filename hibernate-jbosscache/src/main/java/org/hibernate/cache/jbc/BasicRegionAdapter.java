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
package org.hibernate.cache.jbc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.Region;
import org.hibernate.cache.jbc.util.CacheHelper;
import org.hibernate.cache.jbc.util.NonLockingDataVersion;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.Option;
import org.jboss.cache.config.Configuration.NodeLockingScheme;
import org.jboss.cache.notifications.annotation.NodeInvalidated;
import org.jboss.cache.notifications.annotation.NodeModified;
import org.jboss.cache.notifications.annotation.ViewChanged;
import org.jboss.cache.notifications.event.NodeInvalidatedEvent;
import org.jboss.cache.notifications.event.NodeModifiedEvent;
import org.jboss.cache.notifications.event.ViewChangedEvent;
import org.jboss.cache.optimistic.DataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General support for writing {@link Region} implementations for JBoss Cache
 * 2.x.
 * 
 * @author Steve Ebersole
 */
public abstract class BasicRegionAdapter implements Region {
   
    private enum InvalidateState { INVALID, CLEARING, VALID };
    
    public static final String ITEM = CacheHelper.ITEM;

    protected final Cache jbcCache;
    protected final String regionName;
    protected final Fqn regionFqn;
    protected final Fqn internalFqn;
    protected Node regionRoot;
    protected final boolean optimistic;
    protected final TransactionManager transactionManager;
    protected final Logger log;
    protected final Object regionRootMutex = new Object();
    protected final Object memberId;
    protected final boolean replication;
    protected final Object invalidationMutex = new Object();
    protected final AtomicReference<InvalidateState> invalidateState = 
       new AtomicReference<InvalidateState>(InvalidateState.VALID);
    protected final Set<Object> currentView = new HashSet<Object>();

//    protected RegionRootListener listener;
    
    public BasicRegionAdapter(Cache jbcCache, String regionName, String regionPrefix) {

        this.log = LoggerFactory.getLogger(getClass());
        
        this.jbcCache = jbcCache;
        this.transactionManager = jbcCache.getConfiguration().getRuntimeConfig().getTransactionManager();
        this.regionName = regionName;
        this.regionFqn = createRegionFqn(regionName, regionPrefix);
        this.internalFqn = CacheHelper.getInternalFqn(regionFqn);
        this.optimistic = jbcCache.getConfiguration().getNodeLockingScheme() == NodeLockingScheme.OPTIMISTIC;
        this.memberId = jbcCache.getLocalAddress();
        this.replication = CacheHelper.isClusteredReplication(jbcCache);
        
        this.jbcCache.addCacheListener(this);
        
        synchronized (currentView) {
           List view = jbcCache.getMembers();
           if (view != null) {
              currentView.addAll(view);
           }
        }
        
        activateLocalClusterNode();
        
        log.debug("Created Region for " + regionName + " -- regionPrefix is " + regionPrefix);
    }

    protected abstract Fqn<String> createRegionFqn(String regionName, String regionPrefix);

    protected void activateLocalClusterNode() {
       
        // Regions can get instantiated in the course of normal work (e.g.
        // a named query region will be created the first time the query is
        // executed), so suspend any ongoing tx
        Transaction tx = suspend();
        try {
            Configuration cfg = jbcCache.getConfiguration();
            if (cfg.isUseRegionBasedMarshalling()) {
                org.jboss.cache.Region jbcRegion = jbcCache.getRegion(regionFqn, true);
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = getClass().getClassLoader();
                }
                jbcRegion.registerContextClassLoader(classLoader);
                if ( !jbcRegion.isActive() ) {
                    jbcRegion.activate();
                }
            }
            
//            // If we are using replication, we may remove the root node
//            // and then need to re-add it. In that case, the fact
//            // that it is resident will not replicate, so use a listener
//            // to set it as resident
//            if (CacheHelper.isClusteredReplication(cfg.getCacheMode()) 
//                  || CacheHelper.isClusteredInvalidation(cfg.getCacheMode())) {
//                listener = new RegionRootListener();
//                jbcCache.addCacheListener(listener);
//            }
            
            regionRoot = jbcCache.getRoot().getChild( regionFqn );
            if (regionRoot == null || !regionRoot.isValid()) {
               // Establish the region root node with a non-locking data version
               DataVersion version = optimistic ? NonLockingDataVersion.INSTANCE : null;
               regionRoot = CacheHelper.addNode(jbcCache, regionFqn, true, true, version);
            }
            else if (optimistic && regionRoot instanceof NodeSPI) {
                // FIXME Hacky workaround to JBCACHE-1202
                if ( !( ( ( NodeSPI ) regionRoot ).getVersion() instanceof NonLockingDataVersion ) ) {
                    ((NodeSPI) regionRoot).setVersion(NonLockingDataVersion.INSTANCE);
                }
            }
            if (!regionRoot.isResident()) {
               regionRoot.setResident(true);
            }
            establishInternalNodes();
        }
        catch (Exception e) {
            throw new CacheException(e.getMessage(), e);
        }
        finally {
            resume(tx);
        }
        
    }

    private void establishRegionRootNode()
    {
        synchronized (regionRootMutex) {
            // If we've been blocking for the mutex, perhaps another
            // thread has already reestablished the root.
            // In case the node was reestablised via replication, confirm it's 
            // marked "resident" (a status which doesn't replicate)
            if (regionRoot != null && regionRoot.isValid()) {
                return;
            }
            
            // For pessimistic locking, we just want to toss out our ref
            // to any old invalid root node and get the latest (may be null)            
            if (!optimistic) {
               establishInternalNodes();
               regionRoot = jbcCache.getRoot().getChild( regionFqn );
               return;
            }
            
            // The rest only matters for optimistic locking, where we
            // need to establish the proper data version on the region root
            
            // Don't hold a transactional lock for this 
            Transaction tx = suspend();
            Node newRoot = null;
            try {
                 // Make sure the root node for the region exists and 
                 // has a DataVersion that never complains
                 newRoot = jbcCache.getRoot().getChild( regionFqn );
                 if (newRoot == null || !newRoot.isValid()) {                
                     // Establish the region root node with a non-locking data version
                     DataVersion version = optimistic ? NonLockingDataVersion.INSTANCE : null;
                     newRoot = CacheHelper.addNode(jbcCache, regionFqn, true, true, version);    
                 }
                 else if (newRoot instanceof NodeSPI) {
                     // FIXME Hacky workaround to JBCACHE-1202
                     if ( !( ( ( NodeSPI ) newRoot ).getVersion() instanceof NonLockingDataVersion ) ) {
                          ((NodeSPI) newRoot).setVersion(NonLockingDataVersion.INSTANCE);
                     }
                 }
                 // Never evict this node
                 newRoot.setResident(true);
                 establishInternalNodes();
            }
            finally {
                resume(tx);
                regionRoot = newRoot;
            }
        }
    }

    private void establishInternalNodes()
    {
       synchronized (currentView) {
          Transaction tx = suspend();
          try {
             for (Object member : currentView) {
                DataVersion version = optimistic ? NonLockingDataVersion.INSTANCE : null;
                Fqn f = Fqn.fromRelativeElements(internalFqn, member);
                CacheHelper.addNode(jbcCache, f, true, false, version);
             }
          }
          finally {
             resume(tx);
          }
       }
       
    }

    public String getName() {
        return regionName;
    }

    public Cache getCacheInstance() {
        return jbcCache;
    }

    public Fqn getRegionFqn() {
        return regionFqn;
    }
    
    public Object getMemberId()
    {
       return this.memberId;
    }
    
    /**
     * Checks for the validity of the root cache node for this region,
     * creating a new one if it does not exist or is invalid, and also
     * ensuring that the root node is marked as resident.  Suspends any 
     * transaction while doing this to ensure no transactional locks are held 
     * on the region root.
     * 
     * TODO remove this once JBCACHE-1250 is resolved.
     */
    public void ensureRegionRootExists() {
       
       if (regionRoot == null || !regionRoot.isValid())
          establishRegionRootNode();
       
       // Fix up the resident flag
       if (regionRoot != null && regionRoot.isValid() && !regionRoot.isResident())
          regionRoot.setResident(true);
    }
    
    public boolean checkValid()
    {
       boolean valid = invalidateState.get() == InvalidateState.VALID;
       
       if (!valid) {
          synchronized (invalidationMutex) {
             if (invalidateState.compareAndSet(InvalidateState.INVALID, InvalidateState.CLEARING)) {
                Transaction tx = suspend();
                try {
                   Option opt = new Option();
                   opt.setLockAcquisitionTimeout(1);
                   opt.setCacheModeLocal(true);
                   CacheHelper.removeAll(jbcCache, regionFqn, opt);
                   invalidateState.compareAndSet(InvalidateState.CLEARING, InvalidateState.VALID);
                }
                catch (Exception e) {
                   if (log.isTraceEnabled()) {
                      log.trace("Could not invalidate region: " + e.getLocalizedMessage());
                   }
                }
                finally {
                   resume(tx);
                }
             }
          }
          valid = invalidateState.get() == InvalidateState.VALID;
       }
       
       return valid;   
    }

    public void destroy() throws CacheException {
        try {
            // NOTE : this is being used from the process of shutting down a
            // SessionFactory. Specific things to consider:
            // (1) this clearing of the region should not propagate to
            // other nodes on the cluster (if any); this is the
            // cache-mode-local option bit...
            // (2) really just trying a best effort to cleanup after
            // ourselves; lock failures, etc are not critical here;
            // this is the fail-silently option bit...
            Option option = new Option();
            option.setCacheModeLocal(true);
            option.setFailSilently(true);
            if (optimistic) {
                option.setDataVersion(NonLockingDataVersion.INSTANCE);
            }
            jbcCache.getInvocationContext().setOptionOverrides(option);
            jbcCache.removeNode(regionFqn);
            deactivateLocalNode();            
        } catch (Exception e) {
            throw new CacheException(e);
        }
        finally {
            jbcCache.removeCacheListener(this);
        }
    }

    protected void deactivateLocalNode() {
        org.jboss.cache.Region jbcRegion = jbcCache.getRegion(regionFqn, false);
        if (jbcRegion != null && jbcRegion.isActive()) {
            jbcRegion.deactivate();
            jbcRegion.unregisterContextClassLoader();
        }
    }

	public boolean contains(Object key) {
		if ( !checkValid() ) {
			return false;
		}

		try {
			Option opt = new Option();
            opt.setLockAcquisitionTimeout(100);
            CacheHelper.setInvocationOption( jbcCache, opt );
			return CacheHelper.getAllowingTimeout( jbcCache, regionFqn, key ) != null;
		}
		catch ( CacheException ce ) {
			throw ce;
		}
		catch ( Throwable t ) {
			throw new CacheException( t );
		}
	}

    public long getSizeInMemory() {
        // not supported
        return -1;
    }

    public long getElementCountInMemory() {
        if (checkValid()) {
           try {
               Set childrenNames = CacheHelper.getChildrenNames(jbcCache, regionFqn);
               int size = childrenNames.size();
               if (childrenNames.contains(CacheHelper.Internal.NODE)) {
                  size--;
               }
               return size;
           }
		   catch ( CacheException ce ) {
			   throw ce;
		   }
		   catch (Exception e) {
               throw new CacheException(e);
           }
        }
        else {
           return 0;
        }           
    }

    public long getElementCountOnDisk() {
        return -1;
    }

    public Map toMap() {
        if (checkValid()) {
           try {
               Map result = new HashMap();
               Set childrenNames = CacheHelper.getChildrenNames(jbcCache, regionFqn);
               for (Object childName : childrenNames) {
                   if (CacheHelper.Internal.NODE != childName) {
                      result.put(childName, CacheHelper.get(jbcCache,regionFqn, childName));
                   }
               }
               return result;
           } catch (CacheException e) {
               throw e;
           } catch (Exception e) {
               throw new CacheException(e);
           }
        }
        else {
           return Collections.emptyMap();
        }
    }

    public long nextTimestamp() {
        return System.currentTimeMillis() / 100;
    }

    public int getTimeout() {
        return 600; // 60 seconds
    }

    /**
     * Performs a JBoss Cache <code>get(Fqn, Object)</code> after first
     * {@link #suspend suspending any ongoing transaction}. Wraps any exception
     * in a {@link CacheException}. Ensures any ongoing transaction is resumed.
     * 
     * @param key The key of the item to get
     * @param opt any option to add to the get invocation. May be <code>null</code>
     * @param suppressTimeout should any TimeoutException be suppressed?
     * @return The retrieved object
	 * @throws CacheException issue managing transaction or talking to cache
     */
    protected Object suspendAndGet(Object key, Option opt, boolean suppressTimeout) throws CacheException {
        Transaction tx = suspend();
        try {
            CacheHelper.setInvocationOption(getCacheInstance(), opt);
            if (suppressTimeout)
                return CacheHelper.getAllowingTimeout(getCacheInstance(), getRegionFqn(), key);
            else
                return CacheHelper.get(getCacheInstance(), getRegionFqn(), key);
        } finally {
            resume(tx);
        }
    }

    /**
     * Tell the TransactionManager to suspend any ongoing transaction.
     * 
     * @return the transaction that was suspended, or <code>null</code> if
     *         there wasn't one
     */
    public Transaction suspend() {
        Transaction tx = null;
        try {
            if (transactionManager != null) {
                tx = transactionManager.suspend();
            }
        } catch (SystemException se) {
            throw new CacheException("Could not suspend transaction", se);
        }
        return tx;
    }

    /**
     * Tell the TransactionManager to resume the given transaction
     * 
     * @param tx
     *            the transaction to suspend. May be <code>null</code>.
     */
    public void resume(Transaction tx) {
        try {
            if (tx != null)
                transactionManager.resume(tx);
        } catch (Exception e) {
            throw new CacheException("Could not resume transaction", e);
        }
    }

    /**
     * Get an Option with a {@link Option#getDataVersion() data version}
     * of {@link NonLockingDataVersion}.  The data version will not be 
     * set if the cache is not configured for optimistic locking.
     * 
     * @param allowNullReturn If <code>true</code>, return <code>null</code>
     *                        if the cache is not using optimistic locking.
     *                        If <code>false</code>, return a default
     *                        {@link Option}.
     *                        
     * @return the Option, or <code>null</code>.
     */
    protected Option getNonLockingDataVersionOption(boolean allowNullReturn) {
        return optimistic ? NonLockingDataVersion.getInvocationOption() 
                          : (allowNullReturn) ? null : new Option();
    }

    public static Fqn<String> getTypeFirstRegionFqn(String regionName, String regionPrefix, String regionType) {
        Fqn<String> base = Fqn.fromString(regionType);
        Fqn<String> added = Fqn.fromString(escapeRegionName(regionName, regionPrefix));
        return new Fqn<String>(base, added);
    }

    public static Fqn<String> getTypeLastRegionFqn(String regionName, String regionPrefix, String regionType) {
        Fqn<String> base = Fqn.fromString(escapeRegionName(regionName, regionPrefix));
        return new Fqn<String>(base, regionType);
    }

    public static String escapeRegionName(String regionName, String regionPrefix) {
        String escaped = null;
        int idx = -1;
        if (regionPrefix != null) {
            idx = regionName.indexOf(regionPrefix);
        }

        if (idx > -1) {
            int regionEnd = idx + regionPrefix.length();
            String prefix = regionName.substring(0, regionEnd);
            String suffix = regionName.substring(regionEnd);
            suffix = suffix.replace('.', '/');
            escaped = prefix + suffix;
        } else {
            escaped = regionName.replace('.', '/');
            if (regionPrefix != null && regionPrefix.length() > 0) {
                escaped = regionPrefix + "/" + escaped;
            }
        }
        return escaped;
    }
    
    @NodeModified
    public void nodeModified(NodeModifiedEvent event)
    {
       handleEvictAllModification(event);
    }
    
    protected boolean handleEvictAllModification(NodeModifiedEvent event) {
       
       if (!event.isPre() && (replication || event.isOriginLocal()) && event.getData().containsKey(ITEM))
       {
          if (event.getFqn().isChildOf(internalFqn))
          {
             invalidateState.set(InvalidateState.INVALID);
             return true;
          }
       }
       return false;       
    }
    
    @NodeInvalidated
    public void nodeInvalidated(NodeInvalidatedEvent event)
    {
       handleEvictAllInvalidation(event);
    }
    
    protected boolean handleEvictAllInvalidation(NodeInvalidatedEvent event)
    {
       if (!event.isPre() && event.getFqn().isChildOf(internalFqn))
       {
          invalidateState.set(InvalidateState.INVALID);
          return true;
       }      
       return false;
    }
    
    @ViewChanged
    public void viewChanged(ViewChangedEvent event) {
       
       synchronized (currentView) {
          List view = event.getNewView().getMembers();
          if (view != null) {
             currentView.addAll(view);
             establishInternalNodes();
          }
       }
       
    }
   
}
