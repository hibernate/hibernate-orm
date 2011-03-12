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
package org.hibernate.test.cache.jbc.functional;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.ReadWriteCache;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * Common requirement entity caching testing for each
 * {@link org.hibernate.cache.RegionFactory} impl.
 * 
 * @author Steve Ebersole
 */
public abstract class AbstractEntityCacheFunctionalTestCase extends CacheTestCaseBase {

    // note that a lot of the functionality here is intended to be used
    // in creating specific tests for each CacheProvider that would extend
    // from a base test case (this) for common requirement testing...

    public AbstractEntityCacheFunctionalTestCase(String x) {
        super(x);
    }

    @Override
    protected boolean getUseQueryCache() {
        return false;
    }

    public void testEmptySecondLevelCacheEntry() throws Exception {
        getSessions().evictEntity(Item.class.getName());
        Statistics stats = getSessions().getStatistics();
        stats.clear();
        SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(getPrefixedRegionName(Item.class.getName()));
        Map cacheEntries = statistics.getEntries();
        assertEquals(0, cacheEntries.size());
    }

    public void testStaleWritesLeaveCacheConsistent() {
        Session s = openSession();
        Transaction txn = s.beginTransaction();
        VersionedItem item = new VersionedItem();
        item.setName("steve");
        item.setDescription("steve's item");
        s.save(item);
        txn.commit();
        s.close();

        Long initialVersion = item.getVersion();

        // manually revert the version property
        item.setVersion(new Long(item.getVersion().longValue() - 1));

        try {
            s = openSession();
            txn = s.beginTransaction();
            s.update(item);
            txn.commit();
            s.close();
            fail("expected stale write to fail");
        } catch (Throwable expected) {
            // expected behavior here
            if (txn != null) {
                try {
                    txn.rollback();
                } catch (Throwable ignore) {
                }
            }
        } finally {
            if (s != null && s.isOpen()) {
                try {
                    s.close();
                } catch (Throwable ignore) {
                }
            }
        }

        // check the version value in the cache...
        SecondLevelCacheStatistics slcs = sfi().getStatistics().getSecondLevelCacheStatistics(
              getPrefixedRegionName(VersionedItem.class.getName()));

        Object entry = slcs.getEntries().get(item.getId());
        Long cachedVersionValue;
        if (entry instanceof ReadWriteCache.Lock) {
            // FIXME don't know what to test here
            cachedVersionValue = new Long(((ReadWriteCache.Lock) entry).getUnlockTimestamp());
        } else {
            cachedVersionValue = (Long) ((Map) entry).get("_version");
            assertEquals(initialVersion.longValue(), cachedVersionValue.longValue());
        }

        // cleanup
        s = openSession();
        txn = s.beginTransaction();
        item = (VersionedItem) s.load(VersionedItem.class, item.getId());
        s.delete(item);
        txn.commit();
        s.close();

    }
}
