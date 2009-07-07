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

import java.util.Properties;

import org.hibernate.cache.jbc.builder.MultiplexingCacheInstanceManager;

/**
 * {@link JBossCacheRegionFactory} that uses
 * {@link MultiplexingCacheInstanceManager} as its
 * {@link #getCacheInstanceManager() CacheInstanceManager}.
 * <p>
 * Supports separate JBoss Cache instances for entity, collection, query
 * and timestamp caching, with the expectation that a single JGroups resource 
 * (i.e. a multiplexed channel or a shared transport channel) will be shared 
 * between the caches. JBoss Cache instances are created from a factory.
 * </p>
 * <p>
 * This version instantiates the factory itself. See 
 * {@link MultiplexingCacheInstanceManager} for configuration details. 
 * </p>
 * 
 * @author Brian Stansberry
 * @version $Revision$
 */
public class MultiplexedJBossCacheRegionFactory extends JBossCacheRegionFactory {

    /**
     * FIXME Per the RegionFactory class Javadoc, this constructor version
     * should not be necessary.
     * 
     * @param props The configuration properties
     */
    public MultiplexedJBossCacheRegionFactory(Properties props) {
        this();
    }

    /**
     * Create a new MultiplexedJBossCacheRegionFactory.
     * 
     */
    public MultiplexedJBossCacheRegionFactory() {
        super(new MultiplexingCacheInstanceManager());
    }

}
