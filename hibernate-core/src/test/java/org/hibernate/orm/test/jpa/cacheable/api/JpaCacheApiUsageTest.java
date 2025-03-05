/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cacheable.api;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = Order.class,
		integrationSettings = {
				@Setting(name = AvailableSettings.CACHE_REGION_FACTORY, value = "org.hibernate.testing.cache.CachingRegionFactory"),
				@Setting(name = AvailableSettings.JPA_SHARED_CACHE_MODE, value = "ALL"),
				@Setting(name = AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, value = "read-write"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class JpaCacheApiUsageTest {

	@Test
	public void testEviction(EntityManagerFactoryScope scope) {
		//		 first create an Order
		scope.inTransaction(
				entityManager ->
						entityManager.persist( new Order( 1, 500 ) )
		);


		final EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();
		assertTrue( entityManagerFactory.getCache().contains( Order.class, 1 ) );

		scope.inTransaction(
				entityManager -> {
					assertTrue( entityManagerFactory.getCache().contains( Order.class, 1 ) );
					entityManager.createQuery( "delete Order" ).executeUpdate();
				}
		);

		assertFalse( entityManagerFactory.getCache().contains( Order.class, 1 ) );
	}

}
