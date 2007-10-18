package org.hibernate.test.cache.jbc2;

import junit.framework.TestCase;

import org.hibernate.cache.RegionFactory;
import org.hibernate.cache.jbc2.JBossCacheRegionFactory;
import org.hibernate.cache.jbc2.MultiplexedJBossCacheRegionFactory;
import org.hibernate.cache.jbc2.builder.SharedCacheInstanceManager;
import org.hibernate.cache.jbc2.util.CacheHelper;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.test.util.CacheTestSupport;
import org.hibernate.test.util.CacheTestUtil;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.config.Option;
import org.jboss.cache.optimistic.DataVersion;
import org.jboss.cache.transaction.BatchModeTransactionManager;

/**
 * Tests that JBC itself functions as expected in certain areas where there
 * may have been problems in the past.  Basically tests JBC itself, not the 
 * Hibernate/JBC integration.
 * 
 * TODO if the equivalent tests are not in the JBC testsuite, add them.
 * 
 * @author Brian Stansberry
 */
public class JBossCacheComplianceTest extends TestCase {

    private CacheTestSupport testSupport = new CacheTestSupport();
    
    
    public JBossCacheComplianceTest(String x) {
        super(x);
    }

    protected String getConfigResourceKey() {
        return SharedCacheInstanceManager.CACHE_RESOURCE_PROP;
    }

    protected String getConfigResourceLocation() {
        return "org/hibernate/test/cache/jbc2/functional/optimistic-treecache.xml";
    }

    protected Class<? extends RegionFactory> getCacheRegionFactory() {
        return JBossCacheRegionFactory.class;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        testSupport.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        
        testSupport.tearDown();
        
        super.tearDown();
    } 

    @SuppressWarnings("unchecked")
    public void testCacheLevelStaleWritesFail() throws Throwable {
        
        Configuration cfg = CacheTestUtil.buildConfiguration("", MultiplexedJBossCacheRegionFactory.class, true, false);
        cfg.setProperty(getConfigResourceKey(), getConfigResourceLocation());
        
        Settings settings = cfg.buildSettings();
        
        Fqn<String> fqn = Fqn.fromString("/whatever");
        JBossCacheRegionFactory regionFactory = (JBossCacheRegionFactory) settings.getRegionFactory();
        regionFactory.start(settings, cfg.getProperties());
        
        // Make sure we clean up when done
        testSupport.registerFactory(regionFactory);
        
        Cache<Object, Object> treeCache = regionFactory.getCacheInstanceManager().getEntityCacheInstance();

        // Make sure this is an OPTIMISTIC cache
        assertEquals("Cache is OPTIMISTIC", "OPTIMISTIC", treeCache.getConfiguration().getNodeLockingSchemeString());
        
        Long long1 = new Long(1);
        Long long2 = new Long(2);

        try {
            System.out.println("****************************************************************");
            BatchModeTransactionManager.getInstance().begin();
            CacheHelper.setInvocationOption(treeCache, ManualDataVersion.gen(1));
            treeCache.put(fqn, "ITEM", long1);
            BatchModeTransactionManager.getInstance().commit();

            System.out.println("****************************************************************");
            BatchModeTransactionManager.getInstance().begin();
            CacheHelper.setInvocationOption(treeCache, ManualDataVersion.gen(2));
            treeCache.put(fqn, "ITEM", long2);
            BatchModeTransactionManager.getInstance().commit();

            try {
                System.out.println("****************************************************************");
                BatchModeTransactionManager.getInstance().begin();
                CacheHelper.setInvocationOption(treeCache, ManualDataVersion.gen(1));
                treeCache.put(fqn, "ITEM", long1);
                BatchModeTransactionManager.getInstance().commit();
                fail("stale write allowed");
            } catch (Throwable ignore) {
                // expected behavior
                try {
                    BatchModeTransactionManager.getInstance().rollback();
                }
                catch (IllegalStateException ignored) {
                    // tx is already cleared
                }
            }

            Long current = (Long) treeCache.get(fqn, "ITEM");
            assertEquals("unexpected current value", 2, current.longValue());
        } finally {
            try {
                treeCache.remove(fqn, "ITEM");
            } catch (Throwable ignore) {
            }
        }
    }

    private static class ManualDataVersion implements DataVersion {

        /** The serialVersionUID */
        private static final long serialVersionUID = 1L;

        private final int version;

        public ManualDataVersion(int version) {
            this.version = version;
        }

        public boolean newerThan(DataVersion dataVersion) {
            return this.version > ((ManualDataVersion) dataVersion).version;
        }

        public static Option gen(int version) {
            ManualDataVersion mdv = new ManualDataVersion(version);
            Option option = new Option();
            option.setDataVersion(mdv);
            return option;
        }
    }
}
