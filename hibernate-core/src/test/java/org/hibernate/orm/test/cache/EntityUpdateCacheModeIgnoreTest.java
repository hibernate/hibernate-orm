/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.CacheMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that updating an entity does not add an entity to the cache with CacheMode.IGNORE
 */
@DomainModel(
		annotatedClasses = {
				EntityUpdateCacheModeIgnoreTest.PurchaseOrder.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting(name = Environment.USE_QUERY_CACHE, value = "true"),
		}
)
@SessionFactory(generateStatistics = true)
public class EntityUpdateCacheModeIgnoreTest {

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	@JiraKey(value = "HHH9739")
	public void testCacheModeIgnore(SessionFactoryScope scope) {
		// Test that there is no interaction with cache except for invalidation when using CacheMode.IGNORE
		// From API Doc : CacheMode.IGNORE -> The session will never interact with the cache, except to invalidate cache items when updates occur.
		Statistics statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();

		// ----------------------------------------------------------------------------------------------
		// insert
		scope.inTransaction( s -> {
			s.setCacheMode( CacheMode.IGNORE );
			scope.getSessionFactory().getCache().evictAllRegions();

			PurchaseOrder purchaseOrder = new PurchaseOrder( 1L, 2L, 1000L );
			s.persist( purchaseOrder );

		} );

		assertEquals( 0L, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0L, statistics.getSecondLevelCacheMissCount() );
		assertEquals( 0L, statistics.getSecondLevelCachePutCount() );
		assertFalse( scope.getSessionFactory().getCache().containsEntity( PurchaseOrder.class, 1L ) );

		// ----------------------------------------------------------------------------------------------
		// update
		scope.inTransaction( s -> {
			s.setCacheMode( CacheMode.IGNORE );
			scope.getSessionFactory().getCache().evictAllRegions();
			statistics.clear();
			PurchaseOrder result = (PurchaseOrder) s.get( PurchaseOrder.class, 1L );
			result.setTotalAmount( 2000L );
		} );

		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 0, statistics.getSecondLevelCacheMissCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
		// the following fails because the cache contains a lock for that entity
		//assertFalse(sessionFactory.getCache().containsEntity(PurchaseOrder.class, 1L));

		// make sure the updated entity is not found in the cache
		scope.inTransaction( s -> {
			s.setCacheMode( CacheMode.GET );
			var result = s.get( PurchaseOrder.class, 1L );
			assertEquals( 2000, result.getTotalAmount().longValue() );
		} );

		assertEquals( 0, statistics.getSecondLevelCacheHitCount() );
		assertEquals( 1, statistics.getSecondLevelCacheMissCount() );
		assertEquals( 0, statistics.getSecondLevelCachePutCount() );
	}

	@Entity
	@Table(name = "PurchaseOrder")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
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
