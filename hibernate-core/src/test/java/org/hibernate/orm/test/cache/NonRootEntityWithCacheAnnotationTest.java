/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.util.ServiceRegistryUtil;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.SharedCacheMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-11143")
public class NonRootEntityWithCacheAnnotationTest {

	@Test
	public void testCacheOnNonRootEntity() {
		Map<String,Object> settings = new HashMap<>();
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );

		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				.build()) {
			try {
				MetadataBuildingTestHelper.buildMetadata( serviceRegistry, ABase.class, AEntity.class );
				fail("No error for @Cache on subclass entity");
			}
			catch (AnnotationException ae) {
				//exception required
			}
		}
	}

	@Entity
	@Inheritance
	public static class ABase {
		@Id
		private Long id;
	}

	@Entity
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class AEntity extends ABase {
		private String name;
	}
}
