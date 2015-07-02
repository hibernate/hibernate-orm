package org.hibernate.test.cache.infinispan.functional;

import java.util.concurrent.Callable;

import org.hibernate.Session;
import org.hibernate.cache.infinispan.entity.EntityRegionImpl;
import org.infinispan.commons.util.CloseableIteratorSet;
import org.infinispan.context.Flag;
import org.junit.Test;

import static org.infinispan.test.TestingUtil.withTx;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public class NoTenancyTestCase extends SingleNodeTestCase {
    @Test
    public void testNoTenancy() throws Exception {
        final Item item = new Item("my item", "description" );

        long id = withTx(tm, new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                Session s = openSession();
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
                    Session s = openSession();
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
        EntityRegionImpl region = (EntityRegionImpl) sessionFactory().getSecondLevelCacheRegion(Item.class.getName());
        CloseableIteratorSet keySet = region.getCache().withFlags(Flag.CACHE_MODE_LOCAL).keySet();
        assertEquals(1, keySet.size());
        assertEquals(sessionFactory().getClassMetadata(Item.class).getIdentifierType().getReturnedClass(), keySet.iterator().next().getClass());
    }

}
