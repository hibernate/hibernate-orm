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

package org.hibernate.test.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.cache.RegionFactory;
import org.jboss.cache.Cache;

/**
 * Support class for tracking and cleaning up objects used in tests.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public class CacheTestSupport {
    
    private Set<Cache> caches = new HashSet();
    private Set<RegionFactory> factories = new HashSet();
    private Exception exception;
 
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
        cleanUp();
        throwStoredException();
    }

    public void tearDown() throws Exception {       
        cleanUp();
        throwStoredException();
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
                cache.destroy();
            }
            catch (Exception e) {
                storeException(e);
            }
            finally {
                it.remove();
            }
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
