/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.service;

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
		StandardServiceRegistryImpl serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( ConnectionProviderBuilder.getConnectionProviderProperties() )
				.build();
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
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
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
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
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );

		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.setProperty( Environment.DIALECT, H2Dialect.class.getName() );

		serviceRegistry = (StandardServiceRegistryImpl) new StandardServiceRegistryBuilder()
				.applySettings( props )
				.addService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() )
				.build();
		jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( UserSuppliedConnectionProviderImpl.class ) );

		serviceRegistry.destroy();
	}
}
