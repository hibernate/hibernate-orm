/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2007, Red Hat, Inc. and/or it's affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc. and/or it's affiliates.
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
package org.hibernate.test.cache.infinispan.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;

import org.hibernate.cache.RegionFactory;
import org.infinispan.Cache;

/**
 * Support class for tracking and cleaning up objects used in tests.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 */
public class CacheTestSupport {
    
    private static final String PREFER_IPV4STACK = "java.net.preferIPv4Stack";
    
    private Logger log;
    
    private Set<Cache> caches = new HashSet();
    private Set<RegionFactory> factories = new HashSet();
    private Exception exception;
    private String preferIPv4Stack;
 
    public CacheTestSupport(Logger log) {
       this.log = log;
    }
    
    public void registerCache(Cache cache) {
        caches.add(cache);
    }
    
    public void registerFactory(RegionFactory factory) {
        factories.add(factory);
    }
 
    public void unregisterCache(Cache cache) {
        caches.remove(cache);
    }
    
    public void unregisterFactory(RegionFactory factory) {
        factories.remove(factory);
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
       sleep(100);
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
