/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.internal.EntityBinder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.SharedCacheMode;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@JiraKey( value = "HHH-11143")
public class NonRootEntityWithCacheableAnnotationTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( MethodHandles.lookup(), CoreMessageLogger.class, EntityBinder.class.getName() )
	);

	@Test
	public void testCacheableOnNonRootEntity() {
		Map<String,Object> settings = new HashMap<>();
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );

		try (ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySettings( settings )
				.build()) {

			Triggerable triggerable = logInspection.watchForLogMessages( "HHH000482" );

			Metadata metadata = new MetadataSources( serviceRegistry )
					.addAnnotatedClass( ABase.class )
					.addAnnotatedClass( AEntity.class )
					.buildMetadata();

			assertFalse( metadata.getEntityBinding( ABase.class.getName() ).isCached() );
			assertTrue( metadata.getEntityBinding( AEntity.class.getName() ).isCached() );

			assertFalse( triggerable.wasTriggered() );
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
