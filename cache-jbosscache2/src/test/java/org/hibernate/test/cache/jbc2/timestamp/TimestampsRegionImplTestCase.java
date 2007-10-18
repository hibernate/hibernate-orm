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

package org.hibernate.test.cache.jbc2.timestamp;

import java.util.Properties;

import org.hibernate.cache.CacheDataDescription;
import org.hibernate.cache.Region;
import org.hibernate.cache.jbc2.BasicRegionAdapter;
import org.hibernate.cache.jbc2.CacheInstanceManager;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.timestamp.TimestampsRegionImpl;
import org.hibernate.test.cache.jbc2.AbstractRegionImplTestCase;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;

/**
 * Tests of TimestampsRegionImpl.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class TimestampsRegionImplTestCase extends AbstractRegionImplTestCase {
    
    /**
     * Create a new EntityRegionImplTestCase.
     * 
     * @param name
     */
    public TimestampsRegionImplTestCase(String name) {
        super(name);
    }

    @Override
    protected Region createRegion(JBossCacheRegionFactory regionFactory, String regionName, Properties properties, CacheDataDescription cdd) {
        return regionFactory.buildTimestampsRegion(regionName, properties);
    }

    @Override
    protected Cache getJBossCache(JBossCacheRegionFactory regionFactory) {
        CacheInstanceManager mgr = regionFactory.getCacheInstanceManager();
        return mgr.getTimestampsCacheInstance();
    }

    @Override
    protected Fqn getRegionFqn(String regionName, String regionPrefix) {
        return BasicRegionAdapter.getTypeFirstRegionFqn(regionName, regionPrefix, TimestampsRegionImpl.TYPE);
    }
    
    
}
