/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.infinispan.Cache;
import org.jboss.logging.Logger;

import org.hibernate.cache.spi.RegionFactory;

/**
 * Support class for tracking and cleaning up objects used in tests.
 *
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestSupport {
	private static final Logger log = Logger.getLogger( CacheTestSupport.class );

	private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";

    private Set<Cache> caches = new HashSet();
    private Set<RegionFactory> factories = new HashSet();
    private Exception exception;
    private String preferIPv4Stack;

    public void registerCache(Cache cache) {
        caches.add(cache);
    }

    public void registerFactory(RegionFactory factory) {
        factories.add(factory);
    }

    public void unregisterCache(Cache cache) {
        caches.remove( cache );
    }

    public void unregisterFactory(RegionFactory factory) {
        factories.remove( factory );
    }

    public void setUp() throws Exception {
        // Try to ensure we use IPv4; otherwise cluster formation is very slow
        preferIPv4Stack = System.getProperty(PREFER_IPV4STACK);
        System.setProperty(PREFER_IPV4STACK, "true");

        cleanUp();
        throwStoredException();
    }

    public void tearDown() throws Exception {
        if (preferIPv4Stack == null)
            System.clearProperty(PREFER_IPV4STACK);
        else
            System.setProperty(PREFER_IPV4STACK, preferIPv4Stack);

        cleanUp();
        throwStoredException();
    }

    public void avoidConcurrentFlush() {
       // JG 2.6.1 has a problem where calling flush more than once too quickly
       // can result in several second delays
       sleep( 100 );
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch (InterruptedException e) {
            log.warn("Interrupted during sleep", e);
        }
    }

    private void cleanUp() {
        for (Iterator it = factories.iterator(); it.hasNext(); ) {
            try {
                ((RegionFactory) it.next()).stop();
            }
            catch (Exception e) {
                storeException(e);
            }
            finally {
                it.remove();
            }
        }
        factories.clear();

        for (Iterator it = caches.iterator(); it.hasNext(); ) {
            try {
                Cache cache = (Cache) it.next();
                cache.stop();
            }
            catch (Exception e) {
                storeException(e);
            }
            finally {
                it.remove();
            }
            avoidConcurrentFlush();
        }
        caches.clear();
    }

    private void storeException(Exception e) {
        if (this.exception == null) {
            this.exception = e;
        }
    }

    private void throwStoredException() throws Exception {
        if (exception != null) {
            Exception toThrow = exception;
            exception = null;
            throw toThrow;
        }
    }

}
