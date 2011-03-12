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

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class DialectFactoryTest extends TestCase {
	public DialectFactoryTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new TestSuite( DialectFactoryTest.class );
	}

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

	public void testBuildDialectByProperties() {
		Properties props = new Properties();

		try {
			DialectFactory.buildDialect( props, null );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}

		props.setProperty( Environment.DIALECT, "org.hibernate.dialect.HSQLDialect" );
		assertTrue( DialectFactory.buildDialect( props, null ) instanceof HSQLDialect );
	}

	public void testPreregisteredDialects() {
		testDetermination( "HSQL Database Engine", HSQLDialect.class );
		testDetermination( "H2", H2Dialect.class );
		testDetermination( "MySQL", MySQLDialect.class );
		testDetermination( "PostgreSQL", PostgreSQLDialect.class );
		testDetermination( "Apache Derby", DerbyDialect.class );
		testDetermination( "Ingres", IngresDialect.class );
		testDetermination( "ingres", IngresDialect.class );
		testDetermination( "INGRES", IngresDialect.class );
		testDetermination( "Microsoft SQL Server Database", SQLServerDialect.class );
		testDetermination( "Microsoft SQL Server", SQLServerDialect.class );
		testDetermination( "Sybase SQL Server", SybaseASE15Dialect.class );
		testDetermination( "Adaptive Server Enterprise", SybaseASE15Dialect.class );
		testDetermination( "Adaptive Server Anywhere", SybaseAnywhereDialect.class );
		testDetermination( "Informix Dynamic Server", InformixDialect.class );
		testDetermination( "DB2/NT", DB2Dialect.class );
		testDetermination( "DB2/LINUX", DB2Dialect.class );
		testDetermination( "DB2/6000", DB2Dialect.class );
		testDetermination( "DB2/HPUX", DB2Dialect.class );
		testDetermination( "DB2/SUN", DB2Dialect.class );
		testDetermination( "DB2/LINUX390", DB2Dialect.class );
		testDetermination( "DB2/AIX64", DB2Dialect.class );
		testDetermination( "Oracle", 8, Oracle8iDialect.class );
		testDetermination( "Oracle", 9, Oracle9iDialect.class );
		testDetermination( "Oracle", 10, Oracle10gDialect.class );
		testDetermination( "Oracle", 11, Oracle10gDialect.class );
	}

	public void testCustomDialects() {
		DialectFactory.registerDialectResolver( TestingDialects.MyDialectResolver1.class.getName() );
		DialectFactory.registerDialectResolver( TestingDialects.MyDialectResolver2.class.getName() );
		DialectFactory.registerDialectResolver( TestingDialects.ErrorDialectResolver1.class.getName() );
		DialectFactory.registerDialectResolver( TestingDialects.ErrorDialectResolver2.class.getName() );
		DialectFactory.registerDialectResolver( TestingDialects.MyOverridingDialectResolver1.class.getName() );
		DialectFactory.registerDialectResolver( "org.hibernate.dialect.NoSuchDialectResolver" );
		DialectFactory.registerDialectResolver( "java.lang.Object" );


		testDetermination( "MyDatabase1", TestingDialects.MyDialect1.class );
		testDetermination( "MyDatabase2", 1, TestingDialects.MyDialect21.class );
		testDetermination( "MyTrickyDatabase1", TestingDialects.MyDialect1.class );

		// This should be mapped to DB2Dialect by default, but actually it will be
		// my custom dialect because I have registered MyOverridingDialectResolver1.
		testDetermination( "DB2/MySpecialPlatform", TestingDialects.MySpecialDB2Dialect.class );

		try {
			testDetermination( "ErrorDatabase1", Void.TYPE );
			fail();
		}
		catch ( HibernateException e ) {
//			log.info( "Expected SQL error in resolveDialect and ignored", e );
		}

		try {
			testDetermination( "ErrorDatabase2", Void.TYPE );
			fail();
		}
		catch ( HibernateException e ) {
//			log.info( "Expected runtime error in resolveDialect", e );
		}
	}

	public void testDialectNotFound() {
		Properties properties = new Properties();
		try {
			DialectFactory.buildDialect( properties, Mocks.createConnection( "NoSuchDatabase", 666 ) );
			fail();
		}
		catch ( HibernateException e ) {
			assertNull( e.getCause() );
		}
	}

	private void testDetermination(String databaseName, Class clazz) {
		testDetermination( databaseName, -9999, clazz );
	}

	private void testDetermination(String databaseName, int databaseMajorVersion, Class clazz) {
		Properties properties = new Properties();
		Connection conn = Mocks.createConnection( databaseName, databaseMajorVersion );
		assertEquals( clazz, DialectFactory.buildDialect( properties, conn ).getClass() );
	}
}
