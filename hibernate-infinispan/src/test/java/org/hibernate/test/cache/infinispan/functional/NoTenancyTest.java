package org.hibernate.test.cache.infinispan.functional;

import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.hibernate.cache.infinispan.util.Caches;
import org.hibernate.test.cache.infinispan.functional.entities.Item;
import org.infinispan.AdvancedCache;
import org.infinispan.commons.util.CloseableIterable;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NoTenancyTest extends SingleNodeTest {
	 @Override
	 public List<Object[]> getParameters() {
		  return Collections.singletonList(READ_ONLY_INVALIDATION);
	 }

	 @Test
	 public void testNoTenancy() throws Exception {
		  final Item item = new Item("my item", "description" );

		  withTxSession(s -> s.persist(item));
		  for (int i = 0; i < 5; ++i) { // make sure we get something cached
				withTxSession(s -> {
					  Item item2 = s.get(Item.class, item.getId());
					  assertNotNull(item2);
					  assertEquals(item.getName(), item2.getName());
				});

		  }
		  EntityRegionImpl region = (EntityRegionImpl) sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
		  AdvancedCache localCache = region.getCache().withFlags(Flag.CACHE_MODE_LOCAL);
		  assertEquals(1, localCache.size());
		  try (CloseableIterator iterator = localCache.keySet().iterator()) {
			  assertEquals(sessionFactory().getClassMetadata(Item.class).getIdentifierType().getReturnedClass(), iterator.next().getClass());
		  }
	 }
}
