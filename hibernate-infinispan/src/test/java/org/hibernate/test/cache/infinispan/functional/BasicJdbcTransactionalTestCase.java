/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.cache.infinispan.functional;

import javax.transaction.TransactionManager;
import java.util.Map;

import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import org.hibernate.Session;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransactionFactory;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * BasicJdbcTransactionalTestCase.
 *
 * @author Galder Zamarreño
 * @since 3.5
 */
public class BasicJdbcTransactionalTestCase extends SingleNodeTestCase {
	private static final Log log = LogFactory.getLog( BasicJdbcTransactionalTestCase.class );

	@Override
	protected Class<? extends TransactionFactory> getTransactionFactoryClass() {
		return JdbcTransactionFactory.class;
	}

	@Override
	protected TransactionManager getTransactionManager() {
		return null;
	}

	@Test
	public void testCollectionCache() throws Exception {
		Item item = new Item( "chris", "Chris's Item" );
		Item another = new Item( "another", "Owned Item" );
		item.addItem( another );

		Session s = null;
		try {
			s = openSession();
			s.beginTransaction();
			s.persist( item );
			s.persist( another );
			s.getTransaction().commit();
		}
		catch (Exception e) {
			log.error( "Exception", e );
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}

		try {
			s = openSession();
			s.beginTransaction();
			Item loaded = (Item) s.load( Item.class, item.getId() );
			assertEquals( 1, loaded.getItems().size() );
		}
		catch (Exception e) {
			log.error( "Exception", e );
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}

		try {
			s = openSession();
			s.beginTransaction();
			Statistics stats = s.getSessionFactory().getStatistics();
			SecondLevelCacheStatistics cStats = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
			Item loadedWithCachedCollection = (Item) s.load( Item.class, item.getId() );
			stats.logSummary();
			assertEquals( item.getName(), loadedWithCachedCollection.getName() );
			assertEquals( item.getItems().size(), loadedWithCachedCollection.getItems().size() );
			assertEquals( 1, cStats.getHitCount() );
			Map cacheEntries = cStats.getEntries();
			assertEquals( 1, cacheEntries.size() );
		}
		catch (Exception e) {
			log.error( "Exception", e );
			s.getTransaction().rollback();
			throw e;
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testEmptySecondLevelCacheEntry() throws Exception {
      sessionFactory().getCache().evictCollectionRegion( Item.class.getName() + ".items" );
		Statistics stats = sessionFactory().getStatistics();
		stats.clear();
		SecondLevelCacheStatistics statistics = stats.getSecondLevelCacheStatistics( Item.class.getName() + ".items" );
		Map cacheEntries = statistics.getEntries();
		assertEquals( 0, cacheEntries.size() );
	}

}
