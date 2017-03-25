/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.service;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
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
	public void testBasicBuild() {
		// this test requires that SHOW_SQL property isn't passed from the outside (eg. via Gradle)
		final String showSqlPropertyFromOutside = System.getProperty(Environment.SHOW_SQL);
		Assume.assumeFalse("true".equals(showSqlPropertyFromOutside));

		final StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.build();
		try {
			final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
			assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
			final ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
					ConnectionProviderJdbcConnectionAccess.class,
					jdbcServices.getBootstrapJdbcConnectionAccess()
			);
			assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
			assertFalse( jdbcServices.getSqlStatementLogger().isLogToStdout() );
		}
		finally {
			serviceRegistry.destroy();
		}
	}

	@Test
	public void testBuildWithLogging() {
		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.put( Environment.SHOW_SQL, "true" );

		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
			.applySettings( props )
			.build();

		try {
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
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
		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.build();

		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.setProperty( Environment.DIALECT, H2Dialect.class.getName() );

		try {
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
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
			serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
					.applySettings( props )
					.addService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() )
					.build();
			JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

			assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
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
