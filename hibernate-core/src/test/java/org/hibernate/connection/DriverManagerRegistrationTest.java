/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.connection;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Test;

import org.hibernate.internal.util.ClassLoaderHelper;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This test illustrates the problem with calling {@link ClassLoader#loadClass(String)} rather than
 * {@link Class#forName(String, boolean, ClassLoader)} in terms of invoking static ini
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-7272" )
public class DriverManagerRegistrationTest extends BaseUnitTestCase {

	@Test
	public void testDriverRegistrationUsingLoadClassFails() {
		final String driverClassName = "org.hibernate.connection.DriverManagerRegistrationTest$TestDriver1";
		final String url = "jdbc:hibernate:test";

		try {
			determineClassLoader().loadClass( driverClassName );
		}
		catch (ClassNotFoundException e) {
			fail( "Error loading JDBC Driver class : " + e.getMessage() );
		}

		try {
			DriverManager.getDriver( url );
			fail( "This test should have failed to locate JDBC driver per HHH-7272" );
		}
		catch (SQLException expected) {
			// actually this should fail due to the reasons discussed on HHH-7272
		}
	}

	@Test
	public void testDriverRegistrationUsingClassForNameSucceeds() {
		final String driverClassName = "org.hibernate.connection.DriverManagerRegistrationTest$TestDriver2";
		final String url = "jdbc:hibernate:test2";
		try {
			Class.forName( driverClassName, true, determineClassLoader() );
		}
		catch (ClassNotFoundException e) {
			fail( "Error loading JDBC Driver class : " + e.getMessage() );
		}

		try {
			assertNotNull( DriverManager.getDriver( url ) );
		}
		catch (SQLException expected) {
			fail( "Unanticipated failure according to HHH-7272" );
		}
	}

	private static ClassLoader determineClassLoader() {
		ClassLoader cl = ClassLoaderHelper.getContextClassLoader();
		if ( cl == null ) {
			cl = DriverManagerRegistrationTest.class.getClassLoader();
		}
		return cl;
	}

	@AfterClass
	public static void afterwards() {
		try {
			DriverManager.deregisterDriver( TestDriver1.INSTANCE );
		}
		catch (SQLException ignore) {
		}
		try {
			DriverManager.deregisterDriver( TestDriver2.INSTANCE );
		}
		catch (SQLException ignore) {
		}
	}

	public static abstract class AbstractTestJdbcDriver implements Driver {
		public final String matchUrl;

		protected AbstractTestJdbcDriver(String matchUrl) {
			this.matchUrl = matchUrl;
		}

		@Override
		public Connection connect(String url, Properties info) throws SQLException {
			throw new RuntimeException( "Not real driver" );
		}

		@Override
		public boolean acceptsURL(String url) throws SQLException {
			return url.equals( matchUrl );
		}

		@Override
		public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
			return new DriverPropertyInfo[0];
		}

		@Override
		public int getMajorVersion() {
			return 1;
		}

		@Override
		public int getMinorVersion() {
			return 0;
		}

		@Override
		public boolean jdbcCompliant() {
			return false;
		}

		public Logger getParentLogger()
				throws SQLFeatureNotSupportedException {
			throw new SQLFeatureNotSupportedException();
		}
	}

	public static class TestDriver1 extends AbstractTestJdbcDriver {
		public static final TestDriver1 INSTANCE = new TestDriver1( "jdbc:hibernate:test" );

		public TestDriver1(String matchUrl) {
			super( matchUrl );
		}

		static {
			try {
				DriverManager.registerDriver( INSTANCE );
			}
			catch (SQLException e) {
				System.err.println( "Unable to register driver : " + e.getMessage() );
				e.printStackTrace();
			}
		}
	}

	public static class TestDriver2 extends AbstractTestJdbcDriver {
		public static final TestDriver2 INSTANCE = new TestDriver2( "jdbc:hibernate:test2" );

		public TestDriver2(String matchUrl) {
			super( matchUrl );
		}

		static {
			try {
				DriverManager.registerDriver( INSTANCE );
			}
			catch (SQLException e) {
				System.err.println( "Unable to register driver : " + e.getMessage() );
				e.printStackTrace();
			}
		}
	}
}
