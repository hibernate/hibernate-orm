/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cache.ehcache.ConfigSettings;
import org.hibernate.cache.ehcache.internal.SingletonEhcacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-12869")
public class SingletonEhCacheRegionFactoryClasspathConfigurationFileTest {

	@Test
	public void testCacheInitialization() {
		try ( SessionFactoryImplementor sessionFactory = TestHelper.buildStandardSessionFactory(
				builder -> builder.applySetting( AvailableSettings.CACHE_REGION_FACTORY, "ehcache-singleton" )
						.applySetting( ConfigSettings.EHCACHE_CONFIGURATION_RESOURCE_NAME,
								"/hibernate-config/ehcache-configuration.xml" ) ) ) {
			assertNotNull( sessionFactory );
		}
	}
}
