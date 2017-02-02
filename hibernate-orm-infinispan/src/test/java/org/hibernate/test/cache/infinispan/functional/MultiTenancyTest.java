package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.hibernate.test.cache.infinispan.tm.XaConnectionProvider;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class MultiTenancyTest extends SingleNodeTest {

	 private static final String DB1 = "db1";
	 private static final String DB2 = "db2";
	 private final ConnectionProvider db1
			 = new XaConnectionProvider(ConnectionProviderBuilder.buildConnectionProvider(DB1));
	 private final ConnectionProvider db2
			 = new XaConnectionProvider(ConnectionProviderBuilder.buildConnectionProvider(DB2));

	 @Override
	 public List<Object[]> getParameters() {
		  return Collections.singletonList(READ_ONLY_INVALIDATION);
	 }

	 @Override
	 protected void addSettings(Map settings) {
		  super.addSettings( settings );
		  settings.put( Environment.CACHE_KEYS_FACTORY, DefaultCacheKeysFactory.SHORT_NAME );
	 }

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

		  withTxSession(sessionFactory().withOptions().tenantIdentifier(DB1), s -> s.persist(item));

		  for (int i = 0; i < 5; ++i) { // make sure we get something cached
				withTxSession(sessionFactory().withOptions().tenantIdentifier(DB1), s -> {
						  Item item2 = s.get(Item.class, item.getId());
						  assertNotNull(item2);
						  assertEquals(item.getName(), item2.getName());
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
		  AdvancedCache localCache = region.getCache().withFlags(Flag.CACHE_MODE_LOCAL);
		  assertEquals(1, localCache.size());
		  try (CloseableIterator iterator = localCache.keySet().iterator()) {
			  assertEquals("CacheKeyImplementation", iterator.next().getClass().getSimpleName());
		  }
	 }
}
