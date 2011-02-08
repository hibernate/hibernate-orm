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
package org.hibernate.test.cfg.internal;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.internal.JdbcServicesInitiator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.classloading.internal.ClassLoaderServiceInitiator;
import org.hibernate.service.internal.ServicesRegistryImpl;
import org.hibernate.service.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.service.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.service.jdbc.dialect.internal.DialectFactoryInitiator;
import org.hibernate.service.jdbc.dialect.internal.DialectResolverInitiator;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.test.common.ConnectionProviderBuilder;
import org.hibernate.testing.junit.UnitTestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class ServiceBootstrappingTest extends UnitTestCase {
	private ServicesRegistryImpl servicesRegistry;

	public ServiceBootstrappingTest(String string) {
		super( string );
	}

	protected void setUp() {
		List<ServiceInitiator> serviceInitiators = new ArrayList<ServiceInitiator>();
		serviceInitiators.add( ClassLoaderServiceInitiator.INSTANCE );
		serviceInitiators.add( ConnectionProviderInitiator.INSTANCE );
		serviceInitiators.add( DialectResolverInitiator.INSTANCE );
		serviceInitiators.add( DialectFactoryInitiator.INSTANCE );
		serviceInitiators.add( JdbcServicesInitiator.INSTANCE );

		servicesRegistry = new ServicesRegistryImpl( serviceInitiators );
	}

	protected void tearDown() {
		servicesRegistry.destroy();
	}

	public void testBasicBuild() {
		servicesRegistry.initialize( ConnectionProviderBuilder.getConnectionProviderProperties() );
		JdbcServices jdbcServices = servicesRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider() instanceof DriverManagerConnectionProviderImpl );
		assertFalse( jdbcServices.getSqlStatementLogger().isLogToStdout() );
	}

	public void testBuildWithLogging() {
		Properties props = ConnectionProviderBuilder.getConnectionProviderProperties();
		props.put( Environment.SHOW_SQL, "true" );

		servicesRegistry.initialize( props );
		JdbcServices jdbcServices = servicesRegistry.getService( JdbcServices.class );

		assertTrue( jdbcServices.getDialect() instanceof H2Dialect );
		assertTrue( jdbcServices.getConnectionProvider() instanceof DriverManagerConnectionProviderImpl );
		assertTrue( jdbcServices.getSqlStatementLogger().isLogToStdout() );
	}
}
