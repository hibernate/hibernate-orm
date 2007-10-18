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
 * Red Hat Author(s): Steve Ebersole
 */
package org.hibernate.cache.jbc2;

import java.util.Properties;

import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Settings;
import org.jboss.cache.Cache;

/**
 * Acts as a buffer from how instances of {@link Cache} are built/obtained.
 * 
 * @author Steve Ebersole
 */
public interface CacheInstanceManager {
    /**
     * Retrieve a handle to the {@link Cache} instance to be used for storing
     * entity data.
     * 
     * @return The entity data cache instance.
     */
    public Cache getEntityCacheInstance();

    /**
     * Retrieve a handle to the {@link Cache} instance to be used for storing
     * collection data.
     * 
     * @return The collection data cache instance.
     */
    public Cache getCollectionCacheInstance();

    /**
     * Retrieve a handle to the {@link Cache} instance to be used for storing
     * query results.
     * 
     * @return The query result cache instance.
     */
    public Cache getQueryCacheInstance();

    /**
     * Retrieve a handle to the {@link Cache} instance to be used for storing
     * timestamps.
     * 
     * @return The timestamps cache instance.
     */
    public Cache getTimestampsCacheInstance();

    /**
     * Lifecycle callback to perform any necessary initialization of the
     * CacheInstanceManager. Called exactly once during the construction of a
     * {@link org.hibernate.impl.SessionFactoryImpl}.
     * 
     * @param settings
     *            The settings in effect.
     * @param properties
     *            The defined cfg properties
     * @throws CacheException
     *             Indicates problems starting the L2 cache impl; considered as
     *             a sign to stop {@link org.hibernate.SessionFactory} building.
     */
    public void start(Settings settings, Properties properties) throws CacheException;

    /**
     * Lifecycle callback to perform any necessary cleanup of the underlying
     * CacheInstanceManager. Called exactly once during
     * {@link org.hibernate.SessionFactory#close}.
     */
    public void stop();
}
