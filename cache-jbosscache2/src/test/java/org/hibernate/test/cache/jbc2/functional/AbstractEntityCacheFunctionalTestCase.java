package org.hibernate.test.cache.jbc2.functional;

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
        SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics(Item.class.getName());
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
                VersionedItem.class.getName());

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
