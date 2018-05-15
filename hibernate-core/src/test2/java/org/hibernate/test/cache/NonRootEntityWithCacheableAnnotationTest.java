/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.SharedCacheMode;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.annotations.EntityBinder;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-11143")
public class NonRootEntityWithCacheableAnnotationTest {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, EntityBinder.class.getName() )
	);

	@Test
	public void testCacheableOnNonRootEntity() {
		Map settings = new HashMap();
		settings.put( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName() );
		settings.put( AvailableSettings.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "read-write" );
		settings.put( AvailableSettings.JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE );

		ServiceRegistryImplementor serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder()
				.applySettings( settings )
				.build();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000482" );

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( ABase.class )
				.addAnnotatedClass( AEntity.class )
				.buildMetadata();

		assertFalse( metadata.getEntityBinding( ABase.class.getName() ).isCached() );
		assertTrue( metadata.getEntityBinding( AEntity.class.getName() ).isCached() );

		assertFalse( triggerable.wasTriggered() );

		serviceRegistry.destroy();
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
