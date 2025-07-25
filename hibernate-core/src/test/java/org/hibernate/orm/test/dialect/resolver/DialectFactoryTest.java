/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.resolver;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.orm.test.dialect.TestingDialects;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Steve Ebersole
 */
public class DialectFactoryTest extends BaseUnitTestCase {
	private StandardServiceRegistry registry;
	private DialectFactoryImpl dialectFactory;

	@Before
	public void setUp() {
		final BootstrapServiceRegistry bootReg = new BootstrapServiceRegistryBuilder().applyClassLoader(
				DialectFactoryTest.class.getClassLoader()
		).build();
		registry = ServiceRegistryUtil.serviceRegistryBuilder( bootReg ).build();

		dialectFactory = new DialectFactoryImpl();
		dialectFactory.injectServices( (ServiceRegistryImplementor) registry );
	}

	@After
	public void destroy() {
		if ( registry != null ) {
			registry.close();
		}
	}

	@Test
	public void testExplicitShortNameUse() {
		final Map<String, Object> configValues = new HashMap<>();

		configValues.put( Environment.DIALECT, "H2" );
		assertEquals( H2Dialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );

		configValues.put( Environment.DIALECT, "Oracle" );
		assertEquals( OracleDialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );
	}

	@Test
	public void testExplicitlySuppliedDialectClassName() {
		final Map<String, Object> configValues = new HashMap<>();

		configValues.put( Environment.DIALECT, PostgreSQLDialect.class.getName() );
		assertEquals( PostgreSQLDialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );

		configValues.put( Environment.DIALECT, "org.hibernate.dialect.NoSuchDialect" );
		try {
			dialectFactory.buildDialect( configValues, null );
			fail();
		}
		catch ( HibernateException e ) {
			assertEquals( "unexpected exception type", StrategySelectionException.class, e.getClass() );
		}

		configValues.put( Environment.DIALECT, "java.lang.Object" );
		try {
			dialectFactory.buildDialect( configValues, null );
			fail();
		}
		catch ( HibernateException e ) {
			assertEquals( "unexpected exception type", ClassCastException.class, e.getCause().getClass() );
		}
	}

	@Test
	public void testBuildDialectByProperties() {

		try {
			dialectFactory.buildDialect( new HashMap<>(), null );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}

		Map<String,Object> props = new HashMap<>();
		props.put( Environment.DIALECT, PostgreSQLDialect.class.getName() );
		assertEquals( PostgreSQLDialect.class, dialectFactory.buildDialect( props, null ).getClass() );
	}

	@Test
	public void testPreregisteredDialects() {
		DialectResolver resolver = new StandardDialectResolver();
		testDetermination( "HSQL Database Engine", HSQLDialect.class, resolver );
		testDetermination( "H2", H2Dialect.class, resolver );
		testDetermination( "MySQL", MySQLDialect.class, resolver );
		testDetermination( "MySQL", 5, 0, MySQLDialect.class, resolver );
		testDetermination( "MySQL", 5, 5, MySQLDialect.class, resolver );
		testDetermination( "MySQL", 5, 6, MySQLDialect.class, resolver );
		testDetermination( "MySQL", 5, 7, MySQLDialect.class, resolver );
		testDetermination( "MySQL", 8, 0, MySQLDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 10, 3, MariaDBDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 10, 2, MariaDBDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 10, 1, MariaDBDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 10, 0, MariaDBDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 5, 5, MariaDBDialect.class, resolver );
		testDetermination( "MariaDB", "MariaDB connector/J", 5, 2, MariaDBDialect.class, resolver );
		testDetermination( "PostgreSQL", PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 8, 2, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 8, 3, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 0, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 1, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 2, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 3, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 4, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 5, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 6, PostgreSQLDialect.class, resolver );
		testDetermination( "PostgreSQL", 10, 0, PostgreSQLDialect.class, resolver );
		testDetermination( "EnterpriseDB", 9, 2, PostgresPlusDialect.class, resolver );
		testDetermination( "Microsoft SQL Server Database", SQLServerDialect.class, resolver );
		testDetermination( "Microsoft SQL Server", SQLServerDialect.class, resolver );
		testDetermination( "Sybase SQL Server", SybaseASEDialect.class, resolver );
		testDetermination( "Adaptive Server Enterprise", SybaseASEDialect.class, resolver );
		testDetermination( "DB2/NT", DB2Dialect.class, resolver );
		testDetermination( "DB2/LINUX", DB2Dialect.class, resolver );
		testDetermination( "DB2/6000", DB2Dialect.class, resolver );
		testDetermination( "DB2/HPUX", DB2Dialect.class, resolver );
		testDetermination( "DB2/SUN", DB2Dialect.class, resolver );
		testDetermination( "DB2/LINUX390", DB2Dialect.class, resolver );
		testDetermination( "DB2/AIX64", DB2Dialect.class, resolver );
		testDetermination( "DB2 UDB for AS/400", DB2iDialect.class, resolver );
		testDetermination( "DB2 UDB for AS/400", 7, 3, DB2iDialect.class, resolver );
		testDetermination( "Oracle", 8, OracleDialect.class, resolver );
		testDetermination( "Oracle", 9, OracleDialect.class, resolver );
		testDetermination( "Oracle", 10, OracleDialect.class, resolver );
		testDetermination( "Oracle", 11, OracleDialect.class, resolver );
	}

	@Test
	public void testCustomDialects() {
		DialectResolverSet resolvers = new DialectResolverSet();
		resolvers.addResolver( new TestingDialects.MyDialectResolver1() );
		resolvers.addResolver( new TestingDialects.MyDialectResolver2() );
		resolvers.addResolver( new TestingDialects.MyOverridingDialectResolver1() );
		//DialectFactory.registerDialectResolver( "org.hibernate.dialect.NoSuchDialectResolver" );
		//DialectFactory.registerDialectResolver( "java.lang.Object" );

		testDetermination( "MyDatabase1", TestingDialects.MyDialect1.class, resolvers );
		testDetermination( "MyDatabase2", 1, TestingDialects.MyDialect21.class, resolvers );
		testDetermination( "MyTrickyDatabase1", TestingDialects.MyDialect1.class, resolvers );

		// This should be mapped to DB2Dialect by default, but actually it will be
		// my custom dialect because I have registered MyOverridingDialectResolver1.
		testDetermination( "DB2/MySpecialPlatform", TestingDialects.MySpecialDB2Dialect.class, resolvers );

		try {
			testDetermination( "ErrorDatabase1", Void.TYPE, resolvers );
			fail();
		}
		catch ( HibernateException e ) {
		}

		try {
			testDetermination( "ErrorDatabase2", Void.TYPE, resolvers );
			fail();
		}
		catch ( HibernateException e ) {
		}
	}

	@Test
	public void testDialectNotFound() {
		Map properties = Collections.EMPTY_MAP;
		try {
			dialectFactory.buildDialect(
					properties,
					new DialectResolutionInfoSource() {
						@Override
						public DialectResolutionInfo getDialectResolutionInfo() {
							return TestingDialectResolutionInfo.forDatabaseInfo( "NoSuchDatabase", 666 );
						}
					}
			);
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}
	}

	private void testDetermination(String databaseName, Class expected, DialectResolver resolver) {
		testDetermination( databaseName, -9999, expected, resolver );
	}

	private void testDetermination(String databaseName, int databaseMajorVersion, Class expected, DialectResolver resolver) {
		testDetermination( databaseName, databaseMajorVersion, -9999, expected, resolver );
	}

	private void testDetermination(
			final String databaseName,
			final int majorVersion,
			final int minorVersion,
			Class expected,
			DialectResolver resolver) {
		testDetermination( databaseName, null, majorVersion, minorVersion, expected, resolver );
	}

	private void testDetermination(
			final String databaseName,
			final String driverName,
			final int majorVersion,
			final int minorVersion,
			Class expected,
			DialectResolver resolver) {
		dialectFactory.setDialectResolver( resolver );
		Dialect resolved = dialectFactory.buildDialect(
				new HashMap<>(),
				new DialectResolutionInfoSource() {
					@Override
					public DialectResolutionInfo getDialectResolutionInfo() {
						return TestingDialectResolutionInfo.forDatabaseInfo( databaseName, driverName, majorVersion, minorVersion );
					}
				}
		);
		assertEquals( expected, resolved.getClass() );
	}
}
