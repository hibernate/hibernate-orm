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

import org.hibernate.cache.jbc2.builder.JndiMultiplexingCacheInstanceManager;

/**
 * {@link JBossCacheRegionFactory} that uses
 * {@link JndiMultiplexingCacheInstanceManager} as its
 * {@link #getCacheInstanceManager() CacheInstanceManager}.
 * <p>
 * Supports separate JBoss Cache instances for entity, collection, query
 * and timestamp caching, with the expectation that a single multiplexed
 * JGroups channel will be shared between the caches. JBoss Cache instances
 * are created from a factory.
 * </p>
 * <p>
 * This version finds the factory in JNDI. See 
 * {@link JndiMultiplexingCacheInstanceManager} for configuration details. 
 * </p>
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class JndiMultiplexedJBossCacheRegionFactory extends JBossCacheRegionFactory {

    /**
     * FIXME Per the RegionFactory class Javadoc, this constructor version
     * should not be necessary.
     * 
     * @param props
     */
    public JndiMultiplexedJBossCacheRegionFactory(Properties props) {
        this();
    }

    /**
     * Create a new MultiplexedJBossCacheRegionFactory.
     * 
     */
    public JndiMultiplexedJBossCacheRegionFactory() {
        super(new JndiMultiplexingCacheInstanceManager());
    }

}
