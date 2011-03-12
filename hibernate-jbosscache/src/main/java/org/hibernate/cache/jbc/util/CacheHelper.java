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
package org.hibernate.cache.jbc.util;

import java.util.Collections;
import java.util.Set;

import org.hibernate.cache.CacheException;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.InvocationContext;
import org.jboss.cache.Node;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.Option;
import org.jboss.cache.lock.TimeoutException;
import org.jboss.cache.optimistic.DataVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for dealing with JBossCache {@link Configuration.CacheMode}.
 * 
 * @author Steve Ebersole
 * @author Brian Stansberry
 */
public class CacheHelper {

    public static enum Internal { NODE, LOCAL };
    
    /** Key under which items are cached */
    public static final String ITEM = "item";
    /** Key and value used in a hack to create region root nodes */
    public static final String DUMMY = "dummy";
    
    private static final Logger log = LoggerFactory.getLogger(CacheHelper.class);

    /**
     * Disallow external instantiation of CacheHelper.
     */
    private CacheHelper() {
    }

    /**
     * Is this cache participating in a cluster with invalidation?
     * 
     * @param cache
     *            The cache to check.
     * @return True if the cache is configured for synchronous/asynchronous
     *         invalidation; false otherwise.
     */
    public static boolean isClusteredInvalidation(Cache cache) {
        return isClusteredInvalidation(cache.getConfiguration().getCacheMode());
    }

    /**
     * Does this cache mode indicate clustered invalidation?
     * 
     * @param cacheMode
     *            The cache to check
     * @return True if the cache mode is confiogured for
     *         synchronous/asynchronous invalidation; false otherwise.
     */
    public static boolean isClusteredInvalidation(Configuration.CacheMode cacheMode) {
        return cacheMode == Configuration.CacheMode.INVALIDATION_ASYNC
                || cacheMode == Configuration.CacheMode.INVALIDATION_SYNC;
    }

    /**
     * Is this cache participating in a cluster with replication?
     * 
     * @param cache
     *            The cache to check.
     * @return True if the cache is configured for synchronous/asynchronous
     *         invalidation; false otherwise.
     */
    public static boolean isClusteredReplication(Cache cache) {
        return isClusteredReplication(cache.getConfiguration().getCacheMode());
    }

    /**
     * Does this cache mode indicate clustered replication?
     * 
     * @param cacheMode
     *            The cache to check
     * @return True if the cache mode is confiogured for
     *         synchronous/asynchronous invalidation; false otherwise.
     */
    public static boolean isClusteredReplication(Configuration.CacheMode cacheMode) {
        return cacheMode == Configuration.CacheMode.REPL_ASYNC || cacheMode == Configuration.CacheMode.REPL_SYNC;
    }
    
    public static boolean isSynchronous(Cache cache) {
        return isSynchronous(cache.getConfiguration().getCacheMode());
    }
    
    public static boolean isSynchronous(Configuration.CacheMode cacheMode) {
        return cacheMode == Configuration.CacheMode.REPL_SYNC || cacheMode == Configuration.CacheMode.INVALIDATION_SYNC;
    }
    
    public static Set getChildrenNames(Cache cache, Fqn fqn) {
        Node node = cache.getRoot().getChild(fqn);
        return (node != null) ? node.getChildrenNames() : Collections.emptySet();
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>get(Fqn, Object)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     */
    public static Object get(Cache cache, Fqn region, Object key) throws CacheException {
        try {
            return cache.get(new Fqn(region, key), ITEM);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>get(Fqn, Object)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     */
    public static Object getAllowingTimeout(Cache cache, Fqn region, Object key) throws CacheException {
        try {
            return cache.get(new Fqn(region, key), ITEM);        
        } 
        catch (TimeoutException ignored) {
            // ignore it
            return null;
        }
        catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>put(Object, Object)</code>, wrapping
     * any exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param value
     *            data to store in the cache node
     */
    public static void put(Cache cache, Fqn region, Object key, Object value) throws CacheException {

        put(cache, region, key, value, null);
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>put(Object, Object)</code>, wrapping
     * any exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param value
     *            data to store in the cache node
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static void put(Cache cache, Fqn region, Object key, Object value, Option option) throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.put(new Fqn(region, key), ITEM, value);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>put(Object, Object)</code>, ignoring any
     * {@link TimeoutException} and wrapping any other exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param value
     *            data to store in the cache node
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static void putAllowingTimeout(Cache cache, Fqn region, Object key, Object value, Option option) throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.put(new Fqn(region, key), ITEM, value);
        }
        catch (TimeoutException allowed) {
            // ignore it
        }
        catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache
     * <code>putForExternalRead(Object, Object)</code>, wrapping any
     * exception in a {@link CacheException}. Ignores any JBoss Cache
     * {@link TimeoutException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param value
     *            data to store in the cache node
     */
    public static boolean putForExternalRead(Cache cache, Fqn region, Object key, Object value) throws CacheException {

        return putForExternalRead(cache, region, key, value, null);
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache
     * <code>putForExternalRead(Object, Object)</code>, wrapping any
     * exception in a {@link CacheException}. Ignores any JBoss Cache
     * {@link TimeoutException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param value
     *            data to store in the cache node
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static boolean putForExternalRead(Cache cache, Fqn region, Object key, Object value, Option option)
            throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.putForExternalRead(new Fqn(region, key), ITEM, value);
            return true;
        } catch (TimeoutException te) {
            // ignore!
            log.debug("ignoring write lock acquisition failure");
            return false;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>removeNode(Fqn)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     */
    public static void remove(Cache cache, Fqn region, Object key) throws CacheException {

        remove(cache, region, key, null);
    }

    /**
     * Builds an {@link Fqn} from <code>region</code> and <code>key</code>
     * and performs a JBoss Cache <code>removeNode(Fqn)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param key
     *            specific key to append to the <code>region</code> to form
     *            the full Fqn
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static void remove(Cache cache, Fqn region, Object key, Option option) throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.removeNode(new Fqn(region, key));
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Performs a JBoss Cache <code>removeNode(Fqn)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     */
    public static void removeAll(Cache cache, Fqn region) throws CacheException {

        removeAll(cache, region, null);
    }

    /**
     * Performs a JBoss Cache <code>removeNode(Fqn)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static void removeAll(Cache cache, Fqn region, Option option) throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.removeNode(region);
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }

    /**
     * Performs a JBoss Cache <code>removeNode(Fqn)</code>, wrapping any
     * exception in a {@link CacheException}.
     * 
     * @param cache
     *            the cache to invoke on
     * @param region
     *            base Fqn for the cache region
     * @param option
     *            invocation Option to set for this invocation. May be
     *            <code>null</code>.
     */
    public static void removeNode(Cache cache, Fqn region, Object key, Option option) throws CacheException {
        try {
            setInvocationOption(cache, option);
            cache.removeNode(new Fqn(region, key));
        } catch (Exception e) {
            throw new CacheException(e);
        }
    }
    
    public static Node addNode(Cache cache, Fqn fqn, boolean localOnly, boolean resident, DataVersion version)
            throws CacheException {
        try {
            Option option = null;
            if (localOnly || version != null) {
                option = new Option();
                option.setCacheModeLocal(localOnly);
                option.setDataVersion(version);
            }
            
            Node root = cache.getRoot();
            setInvocationOption(cache, option);
            // FIXME hack to work around fact that calling
            // Node added = root.addChild( fqn ); doesn't 
            // properly set the version on the node
            Node added = null;
            if (version == null) {
                added = root.addChild( fqn );
            }
            else {
                cache.put(fqn, DUMMY, DUMMY);
                added = root.getChild(fqn);
            }
            if (resident)
                added.setResident(true);
            return added;
        }
        catch (Exception e) {
            throw new CacheException(e);
        }
    }
    

    /**
     * Assigns the given Option to the cache's {@link InvocationContext}. Does
     * nothing if <code>option</code> is <code>null</code>.
     * 
     * @param cache
     *            the cache. Cannot be <code>null</code>.
     * @param option
     *            the option. May be <code>null</code>.
     * 
     * @see {@link Cache#getInvocationContext()}
     * @see {@link InvocationContext#setOptionOverrides(Option)}
     */
    public static void setInvocationOption(Cache cache, Option option) {
        if (option != null) {
            cache.getInvocationContext().setOptionOverrides(option);
        }
    }

    /**
     * Creates an {@link Option} using the given {@link DataVersion} and passes
     * it to {@link #setInvocationOption(Cache, Option)}.
     * 
     * @param cache
     *            the cache to set the Option on. Cannot be <code>null</code>.
     * @param version
     *            the DataVersion to set. Cannot be <code>null</code>.
     */
    public static void setDataVersionOption(Cache cache, DataVersion version) {
        Option option = new Option();
        option.setDataVersion(version);
        setInvocationOption(cache, option);
    }
    
    public static Fqn getInternalFqn(Fqn region)
    {
       return Fqn.fromRelativeElements(region, Internal.NODE);
    }
    
    public static void sendEvictNotification(Cache cache, Fqn region, Object member, Object key, Option option)
    {
       setInvocationOption(cache, option);
       Fqn f = Fqn.fromRelativeElements(region, Internal.NODE, member  == null ? Internal.LOCAL : member, key);
       cache.put(f, ITEM, DUMMY);
    }
    
    public static void sendEvictAllNotification(Cache cache, Fqn region, Object member, Option option)
    {
       setInvocationOption(cache, option);
       Fqn f = Fqn.fromRelativeElements(region, Internal.NODE, member  == null ? Internal.LOCAL : member);
       cache.put(f, ITEM, DUMMY);
    }
}
