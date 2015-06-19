package org.hibernate.test.cache.infinispan.functional;

import java.util.concurrent.Callable;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.Session;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.test.cache.infinispan.tm.XaConnectionProvider;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.context.Flag;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class MultiTenancyTestCase extends SingleNodeTestCase {

    private static final String DB1 = "db1";
    private static final String DB2 = "db2";
    private final ConnectionProvider db1
          = new XaConnectionProvider(ConnectionProviderBuilder.buildConnectionProvider(DB1));
    private final ConnectionProvider db2
          = new XaConnectionProvider(ConnectionProviderBuilder.buildConnectionProvider(DB2));

    @Override
    protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
        super.configureStandardServiceRegistryBuilder(ssrb);
        ssrb.addService(MultiTenantConnectionProvider.class, new AbstractMultiTenantConnectionProvider() {

            @Override
            protected ConnectionProvider getAnyConnectionProvider() {
                return db1;
            }

            @Override
            protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
                if (DB1.equals(tenantIdentifier)) return db1;
                if (DB2.equals(tenantIdentifier)) return db2;
                throw new IllegalArgumentException();
            }
        });
    }

    @Override
    protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
        super.configureSessionFactoryBuilder(sfb);
        sfb.applyMultiTenancyStrategy(MultiTenancyStrategy.DATABASE);
    }

    @Override
    protected void cleanupTest() throws Exception {
        db1.getConnection().close();
        db2.getConnection().close();
    }

    @Test
    public void testMultiTenancy() throws Exception {
        final Item item = new Item("my item", "description" );

        long id = withTx(tm, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Session s = sessionFactory().withOptions().tenantIdentifier(DB1).openSession();
                s.getTransaction().begin();
                s.persist(item);
                s.getTransaction().commit();
                s.close();
                return item.getId();
            }
        });
        for (int i = 0; i < 5; ++i) { // make sure we get something cached
            withTx(tm, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    Session s = sessionFactory().withOptions().tenantIdentifier(DB1).openSession();
                    s.getTransaction().begin();
                    Item item2 = s.get(Item.class, id);
                    s.getTransaction().commit();
                    s.close();
                    assertNotNull(item2);
                    assertEquals(item.getName(), item2.getName());
                    return null;
                }
            });

        }
        // The table ITEMS is not created in DB2 - we would get just an exception
//        for (int i = 0; i < 5; ++i) { // make sure we get something cached
//            withTx(tm, new Callable<Void>() {
//                @Override
//                public Void call() throws Exception {
//                    Session s = sessionFactory().withOptions().tenantIdentifier(DB2).openSession();
//                    s.getTransaction().begin();
//                    Item item2 = s.get(Item.class, id);
//                    s.getTransaction().commit();
//                    s.close();
//                    assertNull(item2);
//                    return null;
//                }
//            });
//        }
        EntityRegionImpl region = (EntityRegionImpl) sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
        CloseableIteratorSet keySet = region.getCache().withFlags(Flag.CACHE_MODE_LOCAL).keySet();
        assertEquals(1, keySet.size());
        assertEquals("OldCacheKeyImplementation", keySet.iterator().next().getClass().getSimpleName());
    }

}
