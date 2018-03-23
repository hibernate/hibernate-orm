/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache;

import java.util.Collection;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SingleRegisteredProviderTest extends BaseUnitTestCase {
	@Test
	public void testCachingExplicitlyDisabled() {
		final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" )
				.build();

		assertThat( registry.getService( RegionFactory.class ), instanceOf( NoCachingRegionFactory.class ) );
	}

	@Test
	public void testCachingImplicitlyEnabledRegistered() {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build();

		final Collection<Class<? extends RegionFactory>> implementors = bsr
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( RegionFactory.class );

		assertThat( implementors.size(), equalTo( 1 ) );

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "" )
				.build();

		assertThat( ssr.getService( RegionFactory.class ), instanceOf( NoCachingRegionFactory.class ) );
	}

	@Test
	public void testCachingImplicitlyEnabledNoRegistered() {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build();

		final Collection<Class<? extends RegionFactory>> implementors = bsr
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( RegionFactory.class );

		assertThat( implementors.size(), equalTo( 1 ) );

		bsr.getService( StrategySelector.class ).unRegisterStrategyImplementor(
				RegionFactory.class,
				implementors.iterator().next()
		);

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "" )
				.build();

		assertThat( ssr.getService( RegionFactory.class ), instanceOf( NoCachingRegionFactory.class ) );
	}

	@Test
	public void testConnectionsRegistered() {
		final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build();

		final Collection<Class<? extends ConnectionProvider>> implementors = bsr
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( ConnectionProvider.class );

		assertThat( implementors.size(), equalTo( 0 ) );

		bsr.getService( StrategySelector.class ).registerStrategyImplementor(
				ConnectionProvider.class,
				"testing",
				DriverManagerConnectionProviderImpl.class
		);

		final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr ).build();

		final ConnectionProvider configuredProvider = ssr.getService( ConnectionProvider.class );

		assertThat( configuredProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
	}
}
