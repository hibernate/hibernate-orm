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
package org.hibernate.test.cache.jbc.entity;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.EntityRegion;
import org.hibernate.cache.Region;
import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.access.AccessType;
import org.hibernate.cache.jbc.BasicRegionAdapter;
import org.hibernate.cache.jbc.CacheInstanceManager;
import org.hibernate.cache.jbc.JBossCacheRegionFactory;
import org.hibernate.cache.jbc.entity.EntityRegionImpl;
import org.hibernate.test.cache.jbc.AbstractEntityCollectionRegionTestCase;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Tests of EntityRegionImpl.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class EntityRegionImplTestCase extends AbstractEntityCollectionRegionTestCase {
    
    /**
     * Create a new EntityRegionImplTestCase.
     * 
     * @param name
     */
    public EntityRegionImplTestCase(String name) {
        super(name);
    } 
    
    @Override
    protected void supportedAccessTypeTest(RegionFactory regionFactory, Properties properties) {
        
        EntityRegion region = regionFactory.buildEntityRegion("test", properties, null);
        
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
        return regionFactory.buildEntityRegion(regionName, properties, cdd);
    }

    @Override
    protected Cache getJBossCache(JBossCacheRegionFactory regionFactory) {
        CacheInstanceManager mgr = regionFactory.getCacheInstanceManager();
        return mgr.getEntityCacheInstance();
    }

    @Override
    protected Fqn getRegionFqn(String regionName, String regionPrefix) {
        return BasicRegionAdapter.getTypeLastRegionFqn(regionName, regionPrefix, EntityRegionImpl.TYPE);
    }

    @Override
    protected void putInRegion(Region region, Object key, Object value) {
        ((EntityRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).insert(key, value, new Integer(1));
    }

    @Override
    protected void removeFromRegion(Region region, Object key) {
        ((EntityRegion) region).buildAccessStrategy(AccessType.TRANSACTIONAL).remove(key);        
    }  
    
    
}
