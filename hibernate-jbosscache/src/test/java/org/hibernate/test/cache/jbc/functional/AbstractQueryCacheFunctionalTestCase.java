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

/**
 * A QueryCacheEnabledCacheProviderTestCase.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 1 $
 */
public abstract class AbstractQueryCacheFunctionalTestCase extends AbstractEntityCacheFunctionalTestCase {

    /**
     * Create a new QueryCacheEnabledCacheProviderTestCase.
     * 
     * @param x
     */
    public AbstractQueryCacheFunctionalTestCase(String x) {
        super(x);
    }

    @Override
    protected boolean getUseQueryCache() {
        return true;
    }

    public void testQueryCacheInvalidation() {
        Session s = openSession();
        Transaction t = s.beginTransaction();
        Item i = new Item();
        i.setName("widget");
        i.setDescription("A really top-quality, full-featured widget.");
        s.persist(i);
        t.commit();
        s.close();

        SecondLevelCacheStatistics slcs = s.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(
              getPrefixedRegionName(Item.class.getName()));

        assertEquals(slcs.getPutCount(), 1);
        assertEquals(slcs.getElementCountInMemory(), 1);
        assertEquals(slcs.getEntries().size(), 1);

        s = openSession();
        t = s.beginTransaction();
        i = (Item) s.get(Item.class, i.getId());

        assertEquals(slcs.getHitCount(), 1);
        assertEquals(slcs.getMissCount(), 0);

        i.setDescription("A bog standard item");

        t.commit();
        s.close();

        assertEquals(slcs.getPutCount(), 2);

        Object entry = slcs.getEntries().get(i.getId());
        Map map;
        if (entry instanceof ReadWriteCache.Item) {
            map = (Map) ((ReadWriteCache.Item) entry).getValue();
        } else {
            map = (Map) entry;
        }
        assertTrue(map.get("description").equals("A bog standard item"));
        assertTrue(map.get("name").equals("widget"));

        // cleanup
        s = openSession();
        t = s.beginTransaction();
        s.delete(i);
        t.commit();
        s.close();
    }

}
