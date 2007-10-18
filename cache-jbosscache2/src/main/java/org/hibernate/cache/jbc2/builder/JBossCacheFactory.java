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

package org.hibernate.cache.jbc2.builder;

import java.util.Set;

import org.jboss.cache.Cache;

/**
 * Factory and registry for JBoss Cache instances configured using
 * named configurations.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public interface JBossCacheFactory {

    /**
     * Gets all the names of all the configurations of which this object
     * is aware.
     * 
     * @return
     */
    Set getConfigurationNames();
    
    /**
     * Get a cache configured according to the given configuration name.
     * <p>
     * The caller is free to invoke the {@link Cache#create()} and
     * {@link Cache#start()} lifecycle methods on the returned cache, but
     * the @link Cache#stop()} and {@link Cache#destroy()} methods should not
     * be invoked, since it is quite possible other session factories are
     * still using the cache. Use {@link #releaseCache(String)} to notify this 
     * factory that the caller is no longer using a cache; let the factory 
     * control stopping and destroying the underlying cache.
     * </p>
     * 
     * @param configName    the name of the configuration
     * @param create        should the cache be instantiated if it
     *                      hasn't already been?
     * @return              the cache, or <code>null</code> if 
     *                      <code>create</code> is false and the cache hasn't
     *                      been created previously.
     *                      
     * @throws IllegalArgumentException if this object is unaware of 
     *                                  <code>configName</code>
     * @throws Exception if there is a problem instantiating the cache
     */
    Cache getCache(String configName, boolean create) throws Exception;
    
    /**
     * Notifies the factory that the caller is no longer using the given
     * cache.  The factory may perform cleanup operations, such as 
     * stopping and destroying the cache.
     * 
     * @param configName
     */
    void releaseCache(String configName);

}