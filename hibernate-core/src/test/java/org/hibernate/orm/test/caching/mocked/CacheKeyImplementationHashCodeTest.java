/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching.mocked;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.internal.CacheKeyImplementation;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Gail Badner
 */
public class CacheKeyImplementationHashCodeTest {

	@Test
	@JiraKey( value = "HHH-12746")
	public void test() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			Metadata metadata = MetadataBuildingTestHelper.buildMetadata(
					serviceRegistry,
					AnEntity.class,
					AnotherEntity.class
			);
			try ( SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory( metadata )) {
				CacheKeyImplementation anEntityCacheKey = createCacheKeyImplementation(
						1,
						sessionFactory.getRuntimeMetamodels()
								.getMappingMetamodel()
								.getEntityDescriptor( AnEntity.class ),
						sessionFactory
				);
				CacheKeyImplementation anotherEntityCacheKey = createCacheKeyImplementation(
						1,
						sessionFactory.getRuntimeMetamodels()
								.getMappingMetamodel()
								.getEntityDescriptor( AnotherEntity.class ),
						sessionFactory
				);
				assertNotEquals( anEntityCacheKey, anotherEntityCacheKey );
			}
		}
	}

	private CacheKeyImplementation createCacheKeyImplementation(
			int id,
			EntityPersister persister,
			SessionFactoryImplementor sfi) {
		return (CacheKeyImplementation) DefaultCacheKeysFactory.staticCreateEntityKey( id, persister, sfi, "tenant" );
	}

	@Entity(name = "AnEntity")
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity(name = "AnotherEntity")
	@Cache( usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AnotherEntity {
		@Id
		@GeneratedValue
		private int id;
	}

}
