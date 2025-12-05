/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.NoCachingRegionFactory;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Steve Ebersole
 */
public class SingleRegisteredProviderTest {
	@Test
	public void testCachingExplicitlyDisabled() {
		try (final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" )
				.build()) {
			assertThat( registry.getService( RegionFactory.class ) ).isInstanceOf( NoCachingRegionFactory.class );
		}
	}

	@Test
	public void testCachingImplicitlyEnabledRegistered() {
		try (final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build()) {
			final Collection<Class<? extends RegionFactory>> implementors = bsr
					.getService( StrategySelector.class )
					.getRegisteredStrategyImplementors( RegionFactory.class );

			assertThat( implementors.size() ).isEqualTo( 1 );

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "" )
					.build();

			assertThat( ssr.getService( RegionFactory.class ) ).isInstanceOf( NoCachingRegionFactory.class );
		}
	}

	@Test
	public void testCachingImplicitlyEnabledNoRegistered() {
		try (final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build()) {
			final Collection<Class<? extends RegionFactory>> implementors = bsr
					.getService( StrategySelector.class )
					.getRegisteredStrategyImplementors( RegionFactory.class );

			assertThat( implementors.size() ).isEqualTo( 1 );

			bsr.getService( StrategySelector.class ).unRegisterStrategyImplementor(
					RegionFactory.class,
					implementors.iterator().next()
			);

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "" )
					.build();

			assertThat( ssr.getService( RegionFactory.class ) ).isInstanceOf( NoCachingRegionFactory.class );
		}
	}

	@Test
	public void testConnectionsRegistered() {
		try (final BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
				.build()) {
			final Collection<Class<? extends ConnectionProvider>> implementors = bsr
					.getService( StrategySelector.class )
					.getRegisteredStrategyImplementors( ConnectionProvider.class );

			assertThat( implementors.size() ).isEqualTo( 0 );

			bsr.getService( StrategySelector.class ).registerStrategyImplementor(
					ConnectionProvider.class,
					"testing",
					DriverManagerConnectionProvider.class
			);

			final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr ).build();

			final ConnectionProvider configuredProvider = ssr.getService( ConnectionProvider.class );

			assertThat( configuredProvider ).isInstanceOf( DriverManagerConnectionProvider.class );
		}
	}
}
