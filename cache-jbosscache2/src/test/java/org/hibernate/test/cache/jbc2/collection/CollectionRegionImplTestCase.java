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

package org.hibernate.test.cache.jbc2.collection;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CollectionRegion;
import org.hibernate.cache.Region;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.collection.CollectionRegionImpl;
import org.hibernate.test.cache.jbc2.AbstractEntityCollectionRegionTestCase;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Tests of CollectionRegionImpl.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CollectionRegionImplTestCase extends AbstractEntityCollectionRegionTestCase {

    /**
     * Create a new EntityRegionImplTestCase.
     * 
     * @param name
     */
    public CollectionRegionImplTestCase(String name) {
        super(name);
    }
    
    protected void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties) {
        
        CollectionRegion region = regionFactory.buildCollectionRegion("test", properties, null);
        
        assertNull("Got TRANSACTIONAL", region.buildAccessStrategy(AccessType.TRANSACTIONAL).lockRegion());
        
        try
        {
            region.buildAccessStrategy(AccessType.READ_ONLY).lockRegion();
            fail("Did not get READ_ONLY");
        }
        catch (UnsupportedOperationException good) {}
        
        try
        {
            region.buildAccessStrategy(AccessType.NONSTRICT_READ_WRITE);
            fail("Incorrectly got NONSTRICT_READ_WRITE");
        }
        catch (CacheException good) {}
        
        try
        {
            region.buildAccessStrategy(AccessType.READ_WRITE);
            fail("Incorrectly got READ_WRITE");
        }
        catch (CacheException good) {}
    }

    @Override
    protected Region createRegion(JBossCacheRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
        return regionFactory.buildCollectionRegion(regionName, properties, cdd);
    }

    @Override
    protected Cache getJBossCache(JBossCacheRegionFactory regionFactory) {
        CacheInstanceManager mgr = regionFactory.getCacheInstanceManager();
        return mgr.getCollectionCacheInstance();
    }

    @Override
    protected Fqn getRegionFqn(String regionName, String regionPrefix) {
        return BasicRegionAdapter.getTypeLastRegionFqn(regionName, regionPrefix, CollectionRegionImpl.TYPE);
    }
    
    
}
