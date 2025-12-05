/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProvider;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class ServiceBootstrappingTest {

	@Test
	public void testBasicBuild() throws Exception {
		Field globalProperties = Environment.class.getDeclaredField( "GLOBAL_PROPERTIES" );
		globalProperties.setAccessible( true );
		Properties props = (Properties) globalProperties.get( null );
		Object showSql = props.remove( Environment.SHOW_SQL );

		// this test requires that SHOW_SQL property isn't passed from the outside (eg. via Gradle)
		final String showSqlPropertyFromOutside = System.getProperty( Environment.SHOW_SQL );
		assertThat( showSqlPropertyFromOutside ).isNotEqualTo( "true" );

		final StandardServiceRegistryImpl serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			final JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
			assertThat( connectionAccess ).isInstanceOf( ConnectionProviderJdbcConnectionAccess.class );

			ConnectionProviderJdbcConnectionAccess connectionProviderJdbcConnectionAccess = (ConnectionProviderJdbcConnectionAccess) connectionAccess;

			assertThat( connectionProviderJdbcConnectionAccess.getConnectionProvider()
					.isUnwrappableAs( DriverManagerConnectionProvider.class ) ).isTrue();
			assertThat( jdbcServices.getSqlStatementLogger().isLogToStdout() ).isFalse();
		}
		finally {
			if ( showSql != null ) {
				props.put( Environment.SHOW_SQL, showSql );
			}
			serviceRegistry.destroy();
		}
	}

	@Test
	public void testBuildWithLogging() {
		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.SHOW_SQL, "true" )
				.build();

		try {
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			final JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
			assertThat( connectionAccess ).isInstanceOf( ConnectionProviderJdbcConnectionAccess.class );

			ConnectionProviderJdbcConnectionAccess connectionProviderJdbcConnectionAccess = (ConnectionProviderJdbcConnectionAccess) connectionAccess;

			assertThat( connectionProviderJdbcConnectionAccess.getConnectionProvider()
					.isUnwrappableAs( DriverManagerConnectionProvider.class ) )
					.isTrue();
			assertThat( jdbcServices.getSqlStatementLogger().isLogToStdout() ).isTrue();
		}
		finally {
			serviceRegistry.destroy();
		}
	}

	@Test
	public void testBuildWithServiceOverride() {
		StandardServiceRegistryImpl serviceRegistry = ServiceRegistryUtil.serviceRegistry();

		try {
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			final JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
			assertThat( connectionAccess ).isInstanceOf( ConnectionProviderJdbcConnectionAccess.class );

			ConnectionProviderJdbcConnectionAccess connectionProviderJdbcConnectionAccess = (ConnectionProviderJdbcConnectionAccess) connectionAccess;

			assertThat( connectionProviderJdbcConnectionAccess.getConnectionProvider()
					.isUnwrappableAs( DriverManagerConnectionProvider.class ) )
					.isTrue();
		}
		finally {
			serviceRegistry.destroy();
		}

		try {
			serviceRegistry = (StandardServiceRegistryImpl) ServiceRegistryUtil.serviceRegistryBuilder()
					.addService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() )
					.build();
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			final JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
			assertThat( connectionAccess ).isInstanceOf( ConnectionProviderJdbcConnectionAccess.class );

			ConnectionProviderJdbcConnectionAccess connectionProviderJdbcConnectionAccess = (ConnectionProviderJdbcConnectionAccess) connectionAccess;

			assertThat( connectionProviderJdbcConnectionAccess.getConnectionProvider()
					.isUnwrappableAs( UserSuppliedConnectionProviderImpl.class ) )
					.isTrue();
		}
		finally {
			serviceRegistry.destroy();
		}
	}
}
