/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests that updating an entity does not add an entity to the cache with CacheMode.IGNORE
 */
public class EntityUpdateCacheModeIgnoreTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
		configuration.setProperty(AvailableSettings.USE_QUERY_CACHE, "true");
		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				PurchaseOrder.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH9739")
	public void testCachModeIgnore() {
		// Test that there is no interaction with cache except for invalidation when using CacheMode.IGNORE
		// From API Doc : CacheMode.IGNORE -> The session will never interact with the cache, except to invalidate cache items when updates occur.
		Session s;
		Transaction t;
		SessionFactory sessionFactory;
		Statistics statistics;

		// ----------------------------------------------------------------------------------------------
		// insert
		s = openSession();
		s.setCacheMode( CacheMode.IGNORE);
		sessionFactory = s.getSessionFactory();
		sessionFactory.getCache().evictAllRegions();
		statistics = sessionFactory.getStatistics();
		statistics.clear();
		t = s.beginTransaction();

		PurchaseOrder purchaseOrder = new PurchaseOrder(1L, 2L, 1000L);
		s.persist(purchaseOrder);

		t.commit();
		s.close();

		assertEquals(0L, statistics.getSecondLevelCacheHitCount());
		assertEquals(0L, statistics.getSecondLevelCacheMissCount());
		assertEquals(0L, statistics.getSecondLevelCachePutCount());
		assertFalse(sessionFactory.getCache().containsEntity(PurchaseOrder.class, 1L));

		// ----------------------------------------------------------------------------------------------
		// update
		s = openSession();
		s.setCacheMode(CacheMode.IGNORE);
		sessionFactory = s.getSessionFactory();
		sessionFactory.getCache().evictAllRegions();
		statistics = sessionFactory.getStatistics();
		statistics.clear();
		t = s.beginTransaction();

		PurchaseOrder result = (PurchaseOrder)s.get(PurchaseOrder.class, 1L);
		result.setTotalAmount(2000L);

		t.commit();
		s.close();

		assertEquals(0, statistics.getSecondLevelCacheHitCount());
		assertEquals(0, statistics.getSecondLevelCacheMissCount());
		assertEquals(0, statistics.getSecondLevelCachePutCount());
		// the following fails because the cache contains a lock for that entity
		//assertFalse(sessionFactory.getCache().containsEntity(PurchaseOrder.class, 1L));

		// make sure the updated entity is not found in the cache
		s = openSession();
		s.setCacheMode( CacheMode.GET );
		sessionFactory = s.getSessionFactory();
		//sessionFactory.getCache().evictAllRegions();
		t = s.beginTransaction();
		result = s.get( PurchaseOrder.class, 1L );
		assertEquals( 2000, result.getTotalAmount().longValue() );
		t.commit();
		s.close();

		assertEquals( 0, statistics.getSecondLevelCacheHitCount());
		assertEquals( 1, statistics.getSecondLevelCacheMissCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
	}

	@Entity
	@Table(name="PurchaseOrder")
	@Cache(usage= CacheConcurrencyStrategy.READ_WRITE)
	public static class PurchaseOrder implements Serializable {

		private static final long serialVersionUID = 1L;

		@Id
		private Long purchaseOrderId;
		private Long customerId;
		private Long totalAmount;

		public PurchaseOrder() {
		}

		public PurchaseOrder(Long purchaseOrderId, Long customerId, Long totalAmount) {
			this.purchaseOrderId = purchaseOrderId;
			this.customerId = customerId;
			this.totalAmount = totalAmount;
		}

		public Long getPurchaseOrderId() {
			return purchaseOrderId;
		}

		public void setPurchaseOrderId(Long purchaseOrderId) {
			this.purchaseOrderId = purchaseOrderId;
		}

		public Long getCustomerId() {
			return customerId;
		}

		public void setCustomerId(Long customerId) {
			this.customerId = customerId;
		}

		public Long getTotalAmount() {
			return totalAmount;
		}

		public void setTotalAmount(Long totalAmount) {
			this.totalAmount = totalAmount;
		}

	}

}
