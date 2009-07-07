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
package org.hibernate.test.cache.jbc.functional.util;

import java.util.Hashtable;
import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;
import org.hibernate.cfg.Settings;
import org.jboss.cache.CacheManager;
import org.jboss.cache.CacheManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A {@link MultiplexingCacheInstanceManager} that exposes its 
 * CacheManager via a static getter so the test fixture can get ahold
 * of it.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class TestCacheInstanceManager extends MultiplexingCacheInstanceManager {
   
    private static final Logger log = LoggerFactory.getLogger(TestCacheInstanceManager.class);
    
    private static final Hashtable cacheManagers = new Hashtable();
    
    public static CacheManager getTestCacheManager(String name) {
       return (CacheManager) cacheManagers.get(name);
    }
    
    public static void addTestCacheManager(String name,CacheManager manager) {
       cacheManagers.put(name, manager);
    }
    
    public static void clearCacheManagers() {
       for (java.util.Iterator it = cacheManagers.values().iterator(); it.hasNext();) {
          CacheManager cm = (CacheManager) it.next();
          try
          {
             if (cm instanceof CacheManagerImpl)
                 ((CacheManagerImpl) cm).stop();
          }
          catch (Exception e)
          {
             log.error("Exception cleaning up CacheManager " + cm);
          }
       }
       cacheManagers.clear();
    }
    
    private String cacheManagerName;
    private boolean locallyAdded;
    
    /**
     * Create a new TestCacheInstanceManager.
     */
    public TestCacheInstanceManager() {
        super();
    }

    @Override
    public void start(Settings settings, Properties properties) throws CacheException {
       
        cacheManagerName = properties.getProperty(DualNodeTestUtil.NODE_ID_PROP);
        
        CacheManager existing = getTestCacheManager(cacheManagerName);
        locallyAdded = (existing == null);
        if (!locallyAdded) {
           setCacheFactory(existing);
        }
        
        super.start(settings, properties);
        
        if (locallyAdded)
            cacheManagers.put(cacheManagerName, getCacheFactory());
    }

   @Override
   public void stop()
   {
      if (locallyAdded)
          cacheManagers.remove(cacheManagerName);
      
      super.stop();
   }
    
}
