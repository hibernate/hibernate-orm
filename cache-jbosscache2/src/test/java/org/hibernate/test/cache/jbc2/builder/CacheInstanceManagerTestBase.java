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
 * Red Hat Author(s): Brian Stansberry
 */

package org.hibernate.test.cache.jbc2.builder;

import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.test.cache.jbc2.AbstractJBossCacheTestCase;
import org.hibernate.test.util.CacheTestUtil;

/**
 * A CacheInstanceManagerTestBase.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class CacheInstanceManagerTestBase extends AbstractJBossCacheTestCase {

    /**
     * Create a new CacheInstanceManagerTestBase.
     * 
     * @param name
     */
    public CacheInstanceManagerTestBase(String name) {
        super(name);
    }

    protected abstract Class getRegionFactoryClass();
    
    public void testUse2ndLevelCache() throws Exception {
        Configuration cfg = CacheTestUtil.buildConfiguration("", getRegionFactoryClass(), false, true);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        CacheInstanceManager cim = regionFactory.getCacheInstanceManager();
        
        assertNull(cim.getCollectionCacheInstance());
        assertNull(cim.getEntityCacheInstance());
        assertNotNull(cim.getQueryCacheInstance());
        assertNotNull(cim.getTimestampsCacheInstance());
    }
    
    public void testUseQueryCache() throws Exception {
        Configuration cfg = CacheTestUtil.buildConfiguration("", getRegionFactoryClass(), true, false);
        
        JBossCacheRegionFactory regionFactory = CacheTestUtil.startRegionFactory(cfg, getCacheTestSupport());
        
        CacheInstanceManager cim = regionFactory.getCacheInstanceManager();
        
        assertNotNull(cim.getCollectionCacheInstance());
        assertNotNull(cim.getEntityCacheInstance());
        assertNull(cim.getQueryCacheInstance());
        assertNull(cim.getTimestampsCacheInstance());
    }
}
