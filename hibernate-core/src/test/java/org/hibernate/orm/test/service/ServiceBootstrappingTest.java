/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.service;

import java.lang.reflect.Field;
import java.util.Properties;

import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assume;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class ServiceBootstrappingTest extends BaseUnitTestCase {

	@Test
	public void testBasicBuild() throws Exception{
		Field globalProperties = Environment.class.getDeclaredField("GLOBAL_PROPERTIES");
		globalProperties.setAccessible(true);
		Properties props = (Properties) globalProperties.get(null);
		Object showSql = props.remove(Environment.SHOW_SQL);

		// this test requires that SHOW_SQL property isn't passed from the outside (eg. via Gradle)
		final String showSqlPropertyFromOutside = System.getProperty(Environment.SHOW_SQL);
		Assume.assumeFalse("true".equals(showSqlPropertyFromOutside));

		final StandardServiceRegistryImpl serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			final ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
					ConnectionProviderJdbcConnectionAccess.class,
					jdbcServices.getBootstrapJdbcConnectionAccess()
			);
			assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
			assertFalse( jdbcServices.getSqlStatementLogger().isLogToStdout() );
		}
		finally {
			if ( showSql != null ) {
				props.put(Environment.SHOW_SQL, showSql);
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

			final ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
					ConnectionProviderJdbcConnectionAccess.class,
					jdbcServices.getBootstrapJdbcConnectionAccess()
			);
			assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
			assertTrue( jdbcServices.getSqlStatementLogger().isLogToStdout() );
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

			ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
					ConnectionProviderJdbcConnectionAccess.class,
					jdbcServices.getBootstrapJdbcConnectionAccess()
			);
			assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
		}
		finally {
			serviceRegistry.destroy();
		}

		try {
			serviceRegistry = (StandardServiceRegistryImpl) ServiceRegistryUtil.serviceRegistryBuilder()
					.addService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() )
					.build();
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
					ConnectionProviderJdbcConnectionAccess.class,
					jdbcServices.getBootstrapJdbcConnectionAccess()
			);
			assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( UserSuppliedConnectionProviderImpl.class ) );
		}
		finally {
			serviceRegistry.destroy();
		}
	}
}
