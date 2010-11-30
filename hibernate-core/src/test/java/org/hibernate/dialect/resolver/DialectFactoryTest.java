/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.dialect.resolver;

import java.util.Properties;
import java.sql.Connection;

import junit.framework.TestSuite;
import junit.framework.TestCase;
import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.InformixDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.TestingDialects;
import org.hibernate.dialect.Mocks;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseAnywhereDialect;
import org.hibernate.cfg.Environment;
import org.hibernate.service.jdbc.dialect.internal.DialectFactoryImpl;
import org.hibernate.service.jdbc.dialect.internal.DialectResolverSet;
import org.hibernate.service.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.service.jdbc.dialect.spi.DialectResolver;
import org.hibernate.test.common.ServiceRegistryHolder;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DialectFactoryTest extends TestCase {
	private ServiceRegistryHolder serviceRegistryHolder;

	public DialectFactoryTest(String name) {
		super( name );
	}

	protected void setUp() {
		serviceRegistryHolder = new ServiceRegistryHolder( Environment.getProperties() );
	}

	protected void tearDown() {
		if ( serviceRegistryHolder != null ) {
			serviceRegistryHolder.destroy();
		}
	}

	public static Test suite() {
		return new TestSuite( DialectFactoryTest.class );
	}

	// TODO: is it still possible to build a dialect using a class name???
	/*
	public void testBuildDialectByClass() {
		assertEquals(
				HSQLDialect.class,
				DialectFactory.constructDialect( "org.hibernate.dialect.HSQLDialect" ).getClass()
		);

		try {
			DialectFactory.constructDialect( "org.hibernate.dialect.NoSuchDialect" );
			fail();
		}
		catch ( HibernateException e ) {
			assertEquals( "unexpected exception type", e.getCause().getClass(), ClassNotFoundException.class );
		}

		try {
			DialectFactory.constructDialect( "java.lang.Object" );
			fail();
		}
		catch ( HibernateException e ) {
			assertEquals( "unexpected exception type", e.getCause().getClass(), ClassCastException.class );
		}
	}
    */

	public void testBuildDialectByProperties() {
		Properties props = new Properties();

		try {
			getDialectFactoryImpl( new StandardDialectResolver() ).buildDialect( props, null );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}

		props.setProperty( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		assertTrue( getDialectFactoryImpl( new StandardDialectResolver() ).buildDialect( props, null ) instanceof HSQLDialect );
	}

	private DialectFactoryImpl getDialectFactoryImpl(DialectResolver dialectResolver) {
		DialectFactoryImpl dialectFactoryImpl = new DialectFactoryImpl();
		dialectFactoryImpl.setClassLoaderService( serviceRegistryHolder.getClassLoaderService() );
		dialectFactoryImpl.setDialectResolver( dialectResolver );
		return dialectFactoryImpl;
	}

	public void testPreregisteredDialects() {
		DialectResolver resolver = new StandardDialectResolver();
		testDetermination( "HSQL Database Engine", HSQLDialect.class, resolver );
		testDetermination( "H2", H2Dialect.class, resolver );
		testDetermination( "MySQL", MySQLDialect.class, resolver );
		testDetermination( "PostgreSQL", PostgreSQLDialect.class, resolver );
		testDetermination( "Apache Derby", DerbyDialect.class, resolver );
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
		testDetermination( "Oracle", 8, Oracle8iDialect.class, resolver );
		testDetermination( "Oracle", 9, Oracle9iDialect.class, resolver );
		testDetermination( "Oracle", 10, Oracle10gDialect.class, resolver );
		testDetermination( "Oracle", 11, Oracle10gDialect.class, resolver );
	}

	public void testCustomDialects() {
		DialectResolverSet resolvers = new DialectResolverSet();
		resolvers.addResolver( new TestingDialects.MyDialectResolver1() );
		resolvers.addResolver( new TestingDialects.MyDialectResolver2() );
		resolvers.addResolver( new TestingDialects.ErrorDialectResolver1() );
		resolvers.addResolver( new TestingDialects.ErrorDialectResolver2() );
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
//			log.info( "Expected SQL error in resolveDialect and ignored", e );
		}

		try {
			testDetermination( "ErrorDatabase2", Void.TYPE, resolvers );
			fail();
		}
		catch ( HibernateException e ) {
//			log.info( "Expected runtime error in resolveDialect", e );
		}
	}

	public void testDialectNotFound() {
		Properties properties = new Properties();
		try {
			getDialectFactoryImpl( new StandardDialectResolver() ).buildDialect( properties, Mocks.createConnection( "NoSuchDatabase", 666 ) );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}
	}

	private void testDetermination(String databaseName, Class clazz, DialectResolver resolver) {
		testDetermination( databaseName, -9999, clazz, resolver );
	}

	private void testDetermination(String databaseName, int databaseMajorVersion, Class clazz, DialectResolver resolver) {
		DialectFactoryImpl dialectFactoryImpl = getDialectFactoryImpl( new StandardDialectResolver() );
		dialectFactoryImpl.setDialectResolver( resolver );
		Properties properties = new Properties();
		Connection conn = Mocks.createConnection( databaseName, databaseMajorVersion );
		assertEquals( clazz, dialectFactoryImpl.buildDialect( properties, conn ).getClass() );
	}
}
