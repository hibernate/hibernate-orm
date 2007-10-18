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

package org.hibernate.cache.jbc2;

import java.util.Properties;

import org.hibernate.cache.jbc2.builder.JndiSharedCacheInstanceManager;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.jboss.cache.DefaultCacheFactory;

/**
 * {@link JBossCacheRegionFactory} that uses
 * {@link SharedCacheInstanceManager} as its
 * {@link #getCacheInstanceManager() CacheInstanceManager}.
 * <p>
 * Basically, uses a single shared JBoss Cache for entities, collections,
 * queries and timestamps. The JBoss Cache instance created by the
 * JBC {@link DefaultCacheFactory} using the resource identified by the
 * {@link JndiSharedCacheInstanceManager#CACHE_RESOURCE_PROP}
 * configuration property. 
 * </p>
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class SharedJBossCacheRegionFactory extends JBossCacheRegionFactory {

    /**
     * FIXME Per the RegionFactory class Javadoc, this constructor version
     * should not be necessary.
     * 
     * @param props
     */
    public SharedJBossCacheRegionFactory(Properties props) {
        this();
    }

    /**
     * Create a new MultiplexedJBossCacheRegionFactory.
     * 
     */
    public SharedJBossCacheRegionFactory() {
        super(new SharedCacheInstanceManager());
    }

}
