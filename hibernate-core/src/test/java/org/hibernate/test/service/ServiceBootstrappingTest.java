/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.service;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentInitiator.ConnectionProviderJdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.env.ConnectionProviderBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class ServiceBootstrappingTest extends BaseUnitTestCase {
	@Test
	public void testBasicBuild() {
		final StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.build();
		final JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );
		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		final ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
		assertFalse( jdbcServices.getSqlStatementLogger().isLogToStdout() );

		serviceRegistry.destroy();
	}

	@Test
	public void testBuildWithLogging() {
		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.put( Environment.SHOW_SQL, "true" );

		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( props )
				.build();

		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		final ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
		assertTrue( jdbcServices.getSqlStatementLogger().isLogToStdout() );

		serviceRegistry.destroy();
	}

	@Test
	public void testBuildWithServiceOverride() {
		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.build();
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		ConnectionProviderJdbcConnectionAccess connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );

		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.setProperty( Environment.DIALECT, H2Dialect.class.getName() );

		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( props )
				.addService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() )
				.build();
		jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		connectionAccess = assertTyping(
				ConnectionProviderJdbcConnectionAccess.class,
				jdbcServices.getBootstrapJdbcConnectionAccess()
		);
		assertTrue( connectionAccess.getConnectionProvider().isUnwrappableAs( UserSuppliedConnectionProviderImpl.class ) );

		serviceRegistry.destroy();
	}
}
