/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.infinispan.functional.classloader;

import java.util.HashSet;
import java.util.Set;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryVisited;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryVisitedEvent;
import org.jboss.logging.Logger;

@Listener
public class CacheAccessListener {
	private static final Logger log = Logger.getLogger( CacheAccessListener.class );

	HashSet modified = new HashSet();
    HashSet accessed = new HashSet();

    public void clear() {
        modified.clear();
        accessed.clear();
    }

    @CacheEntryModified
    public void nodeModified( CacheEntryModifiedEvent event ) {
        if (!event.isPre()) {
            Object key = event.getKey();
            log.info("Modified node " + key);
            modified.add(key.toString());
        }
    }

    @CacheEntryCreated
    public void nodeCreated( CacheEntryCreatedEvent event ) {
        if (!event.isPre()) {
            Object key = event.getKey();
            log.info("Created node " + key);
            modified.add(key.toString());
        }
    }

    @CacheEntryVisited
    public void nodeVisited( CacheEntryVisitedEvent event ) {
        if (!event.isPre()) {
            Object key = event.getKey();
            log.info("Visited node " + key);
            accessed.add(key.toString());
        }
    }

    public boolean getSawRegionModification( Object key ) {
        return getSawRegion(key, modified);
    }

    public int getSawRegionModificationCount() {
        return modified.size();
    }

    public void clearSawRegionModification() {
        modified.clear();
    }

    public boolean getSawRegionAccess( Object key ) {
        return getSawRegion(key, accessed);
    }

    public int getSawRegionAccessCount() {
        return accessed.size();
    }

    public void clearSawRegionAccess() {
        accessed.clear();
    }

    private boolean getSawRegion( Object key,
                                  Set sawEvents ) {
        if (sawEvents.contains(key)) {
            sawEvents.remove(key);
            return true;
        }
        return false;
    }

}
