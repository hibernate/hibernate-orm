/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.TriggerOnPrefixLogListener;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.util.ServiceRegistryUtil;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.SharedCacheMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-11143")
public class NonRootEntityWithCacheableAnnotationTest {

	@Test
	public void testCacheableOnNonRootEntity() {
		Map<String,Object> settings = new HashMap<>();
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );

		try (ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				.build()) {

			TriggerOnPrefixLogListener trigger = new TriggerOnPrefixLogListener( Set.of( "HHH000482" ) );
			LogInspectionHelper.registerListener( trigger, CoreMessageLogger.CORE_LOGGER );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( ABase.class )
					.addAnnotatedClass( AEntity.class )
					.buildMetadata();

			assertFalse( metadata.getEntityBinding( ABase.class.getName() ).isCached() );
			assertTrue( metadata.getEntityBinding( AEntity.class.getName() ).isCached() );

			assertFalse( trigger.wasTriggered() );
		}
	}

	@Entity
	@Inheritance
	public static class ABase {
		@Id
		private Long id;
	}

	@Entity
	@Cacheable
	public static class AEntity extends ABase {
		private String name;
	}
}
