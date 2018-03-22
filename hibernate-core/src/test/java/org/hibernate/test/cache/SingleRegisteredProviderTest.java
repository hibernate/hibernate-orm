/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cache;

import java.util.Collection;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cache.internal.EnabledCaching;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
public class SingleRegisteredProviderTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_PREFIX, "" );
		ssrb.applySetting( AvailableSettings.CACHE_REGION_FACTORY, "" );

		ssrb.applySetting( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	@Override
	protected void configureBootstrapServiceRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {
		super.configureBootstrapServiceRegistryBuilder( bsrb );
		bsrb.applyStrategySelector( ConnectionProvider.class, "testing", DriverManagerConnectionProviderImpl.class );
	}

	@Test
	public void testCachingExpectations() {
		final Collection<Class<? extends RegionFactory>> implementors = sessionFactory().getServiceRegistry()
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( RegionFactory.class );

		assertThat( implementors.size(), equalTo( 1 ) );
		assertThat( sessionFactory().getSessionFactoryOptions().isSecondLevelCacheEnabled(), equalTo( true ) );
		assertThat( sessionFactory().getCache(), instanceOf( EnabledCaching.class ) );
		assertThat( sessionFactory().getCache().getRegionFactory(), instanceOf( CachingRegionFactory.class ) );
		assertThat( implementors.iterator().next(), equalTo( CachingRegionFactory.class ) );
	}

	@Test
	public void testConnectionsExpectations() {
		final Collection<Class<? extends ConnectionProvider>> implementors = sessionFactory().getServiceRegistry()
				.getService( StrategySelector.class )
				.getRegisteredStrategyImplementors( ConnectionProvider.class );

		assertThat( implementors.size(), equalTo( 1 ) );

		final ConnectionProvider configuredProvider = sessionFactory().getServiceRegistry().getService( ConnectionProvider.class );

		assertThat( configuredProvider, instanceOf( DriverManagerConnectionProviderImpl.class ) );
		assertThat( implementors.iterator().next(), equalTo( DriverManagerConnectionProviderImpl.class ) );
	}
}
