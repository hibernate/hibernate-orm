/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.dialect.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.spi.StrategySelectionException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.DB2400Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyTenFiveDialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.DerbyTenSixDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.dialect.TestingDialects;
import org.hibernate.engine.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.engine.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfoSource;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class DialectFactoryTest extends BaseUnitTestCase {
	private StandardServiceRegistry registry;
	private DialectFactoryImpl dialectFactory;

	@Before
	public void setUp() {
		final BootstrapServiceRegistry bootReg = new BootstrapServiceRegistryBuilder().with( DialectFactoryTest.class.getClassLoader() ).build();
		registry = new StandardServiceRegistryBuilder( bootReg ).build();

		dialectFactory = new DialectFactoryImpl();
		dialectFactory.injectServices( (ServiceRegistryImplementor) registry );
	}

	@Test
	public void testExplicitShortNameUse() {
		final Map<String, String> configValues = new HashMap<String, String>();

		configValues.put( Environment.DIALECT, "H2" );
		assertEquals( H2Dialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );

		configValues.put( Environment.DIALECT, "Oracle10g" );
		assertEquals( Oracle10gDialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );
	}

	@Test
	public void testExplicitlySuppliedDialectClassName() {
		final Map<String, String> configValues = new HashMap<String, String>();

		configValues.put( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		assertEquals( HSQLDialect.class, dialectFactory.buildDialect( configValues, null ).getClass() );

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
		Properties props = new Properties();

		try {
			dialectFactory.buildDialect( props, null );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}

		props.setProperty( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		assertEquals( HSQLDialect.class, dialectFactory.buildDialect( props, null ).getClass() );
	}

	@Test
	public void testPreregisteredDialects() {
		DialectResolver resolver = StandardDialectResolver.INSTANCE;
		testDetermination( "HSQL Database Engine", HSQLDialect.class, resolver );
		testDetermination( "H2", H2Dialect.class, resolver );
		testDetermination( "MySQL", MySQLDialect.class, resolver );
		testDetermination( "MySQL", 5, 0, MySQL5Dialect.class, resolver );
		testDetermination( "PostgreSQL", PostgreSQL81Dialect.class, resolver );
		testDetermination( "PostgreSQL", 8, 2, PostgreSQL82Dialect.class, resolver );
		testDetermination( "PostgreSQL", 9, 0, PostgreSQL9Dialect.class, resolver );
		testDetermination( "EnterpriseDB", 9, 2, PostgresPlusDialect.class, resolver );
		testDetermination( "Apache Derby", 10, 4, DB2Dialect.class, resolver );
		testDetermination( "Apache Derby", 10, 5, DerbyTenFiveDialect.class, resolver );
		testDetermination( "Apache Derby", 10, 6, DerbyTenSixDialect.class, resolver );
		testDetermination( "Apache Derby", 11, 5, DerbyTenSevenDialect.class, resolver );
		testDetermination( "Ingres", IngresDialect.class, resolver );
		testDetermination( "ingres", IngresDialect.class, resolver );
		testDetermination( "INGRES", IngresDialect.class, resolver );
		testDetermination( "Microsoft SQL Server Database", SQLServerDialect.class, resolver );
		testDetermination( "Microsoft SQL Server", SQLServerDialect.class, resolver );
		testDetermination( "Sybase SQL Server", SybaseASE15Dialect.class, resolver );
		testDetermination( "Adaptive Server Enterprise", SybaseASE15Dialect.class, resolver );
		testDetermination( "Adaptive Server Anywhere", SybaseAnywhereDialect.class, resolver );
		testDetermination( "Informix Dynamic Server", InformixDialect.class, resolver );
		testDetermination( "DB2/NT", DB2Dialect.class, resolver );
		testDetermination( "DB2/LINUX", DB2Dialect.class, resolver );
		testDetermination( "DB2/6000", DB2Dialect.class, resolver );
		testDetermination( "DB2/HPUX", DB2Dialect.class, resolver );
		testDetermination( "DB2/SUN", DB2Dialect.class, resolver );
		testDetermination( "DB2/LINUX390", DB2Dialect.class, resolver );
		testDetermination( "DB2/AIX64", DB2Dialect.class, resolver );
		testDetermination( "DB2 UDB for AS/400", DB2400Dialect.class, resolver );
		testDetermination( "Oracle", 8, Oracle8iDialect.class, resolver );
		testDetermination( "Oracle", 9, Oracle9iDialect.class, resolver );
		testDetermination( "Oracle", 10, Oracle10gDialect.class, resolver );
		testDetermination( "Oracle", 11, Oracle10gDialect.class, resolver );
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
		dialectFactory.setDialectResolver( resolver );
		Dialect resolved = dialectFactory.buildDialect(
				new Properties(),
				new DialectResolutionInfoSource() {
					@Override
					public DialectResolutionInfo getDialectResolutionInfo() {
						return TestingDialectResolutionInfo.forDatabaseInfo( databaseName, majorVersion, minorVersion );
					}
				}
		);
		assertEquals( expected, resolved.getClass() );
	}
}
