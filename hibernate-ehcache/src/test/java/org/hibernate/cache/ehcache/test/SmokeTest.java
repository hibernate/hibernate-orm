/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.ehcache.test;

import java.util.Collection;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.ehcache.internal.EhcacheRegionFactory;
import org.hibernate.cache.ehcache.internal.SingletonEhcacheRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class SmokeTest {
	@Test
	public void testStrategySelectorRegistrations() {
		final BootstrapServiceRegistry registry = new BootstrapServiceRegistryBuilder().build();
		final Collection<Class<? extends RegionFactory>> implementors = registry
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( RegionFactory.class );
		assertTrue( implementors.contains( EhcacheRegionFactory.class ) );
		assertTrue( implementors.contains( SingletonEhcacheRegionFactory.class ) );
	}

	@Test
	public void testEhcacheShortName() {
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.CACHE_REGION_FACTORY, "ehcache" )
				.build();
		assertThat(
				registry.getService( RegionFactory.class ),
				instanceOf( EhcacheRegionFactory.class )
		);
	}

	@Test
	public void testSingletonEhcacheShortName() {
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.CACHE_REGION_FACTORY, "ehcache-singleton" )
				.build();
		assertThat(
				registry.getService( RegionFactory.class ),
				instanceOf( SingletonEhcacheRegionFactory.class )
		);
	}
}
