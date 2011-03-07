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

import java.util.Properties;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.internal.ServiceRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.common.ConnectionProviderBuilder;

/**
 * @author Steve Ebersole
 */
public class ServiceBootstrappingTest extends BaseUnitTestCase {
	@Test
	public void testBasicBuild() {
		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( ConnectionProviderBuilder.getConnectionProviderProperties() );
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

		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( props );
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );
		assertTrue( jdbcServices.getSqlStatementLogger().isLogToStdout() );

		serviceRegistry.destroy();
	}

	@Test
	public void testBuildWithServiceOverride() {
		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();

		ServiceRegistryImpl serviceRegistry = new ServiceRegistryImpl( props );
		JdbcServices jdbcServices = serviceRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( DriverManagerConnectionProviderImpl.class ) );

		serviceRegistry.registerService( ConnectionProvider.class, new UserSuppliedConnectionProviderImpl() );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider().isUnwrappableAs( UserSuppliedConnectionProviderImpl.class ) );

		serviceRegistry.destroy();
	}
}
