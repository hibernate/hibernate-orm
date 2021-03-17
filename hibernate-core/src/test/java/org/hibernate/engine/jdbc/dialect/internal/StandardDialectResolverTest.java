/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import org.hibernate.dialect.*;
import org.hibernate.dialect.resolver.TestingDialectResolutionInfo;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test of the {@link StandardDialectResolver} class.
 * 
 * @author Bryan Turner
 */
public class StandardDialectResolverTest extends BaseUnitTestCase {

	@Test
	public void testResolveDialectInternalForSQLServer2000() 
			throws SQLException {
		runSQLServerDialectTest( 8, SQLServerDialect.class );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2005() 
			throws SQLException {
		runSQLServerDialectTest( 9, SQLServer2005Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2008() 
			throws SQLException {
		runSQLServerDialectTest( 10, SQLServer2008Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2012() 
			throws SQLException {
		runSQLServerDialectTest( 11, SQLServer2012Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2014()
			throws SQLException {
		runSQLServerDialectTest( 12, SQLServer2012Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForUnknownSQLServerVersion() 
			throws SQLException {
		runSQLServerDialectTest( 7, SQLServerDialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres81() 
			throws SQLException {
		runPostgresDialectTest( 8, 1, PostgreSQL81Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres82() 
			throws SQLException {
		runPostgresDialectTest( 8, 2, PostgreSQL82Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres83() throws SQLException {
		runPostgresDialectTest( 8, 3, PostgreSQL82Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres84() throws SQLException {
		runPostgresDialectTest( 8, 4, PostgreSQL82Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres9() throws SQLException {
		runPostgresDialectTest( 9, 0, PostgreSQL9Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres91() throws SQLException {
		runPostgresDialectTest( 9, 1, PostgreSQL9Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForPostgres92() throws SQLException {
		runPostgresDialectTest( 9, 2, PostgreSQL92Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB103() throws SQLException {
		runMariaDBDialectTest( 10, 3, MariaDB103Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB102() throws SQLException {
		runMariaDBDialectTest( 10, 2, MariaDB102Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB101() throws SQLException {
		runMariaDBDialectTest( 10, 1, MariaDB10Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB100() throws SQLException {
		runMariaDBDialectTest( 10, 0, MariaDB10Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB55() throws SQLException {
		runMariaDBDialectTest( 5, 5, MariaDB53Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMariaDB52() throws SQLException {
		runMariaDBDialectTest( 5, 2, MariaDBDialect.class );
	}

	@Test
	public void testResolveDialectInternalForMySQL57() throws SQLException {
		runMySQLDialectTest( 5, 7, MySQL57Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMySQL6() throws SQLException {
		runMySQLDialectTest( 6, 0, MySQL57Dialect.class );
	}

	@Test
	public void testResolveDialectInternalForMySQL7() throws SQLException {
		runMySQLDialectTest( 7, 0, MySQL57Dialect.class );
	}


	@Test
	public void testResolveDialectInternalForMySQL8() throws SQLException {
		runMySQLDialectTest( 8, 0, MySQL8Dialect.class );
	}

	private static void runMariaDBDialectTest(
			int majorVersion, int minorVersion, Class<? extends MariaDBDialect> expectedDialect)
			throws SQLException {
		runDialectTest( "MariaDB", "MariaDB connector/J", majorVersion, minorVersion, expectedDialect );
	}

	private static void runMySQLDialectTest(
			int majorVersion, int minorVersion, Class<? extends MySQLDialect> expectedDialect)
			throws SQLException {
		runDialectTest( "MySQL", "MySQL connector/J", majorVersion, minorVersion, expectedDialect );
	}

	private static void runSQLServerDialectTest(
			int version, Class<? extends SQLServerDialect> expectedDialect)
			throws SQLException {
		runDialectTest( "Microsoft SQL Server", version, 0,
				expectedDialect );
	}

	private static void runPostgresDialectTest(
			int majorVersion, int minorVersion,
			Class<? extends Dialect> expectedDialect) throws SQLException {
		runDialectTest( "PostgreSQL", majorVersion, minorVersion,
				expectedDialect );
	}

	private static void runDialectTest(
			String productName,
			int majorVersion,
			int minorVersion,
			Class<? extends Dialect> expectedDialect) {
		runDialectTest( productName, null, majorVersion, minorVersion, expectedDialect );
	}

	private static void runDialectTest(
			String productName,
			String driverName,
			int majorVersion,
			int minorVersion,
			Class<? extends Dialect> expectedDialect) {
		TestingDialectResolutionInfo info = TestingDialectResolutionInfo.forDatabaseInfo( productName, driverName, majorVersion, minorVersion );

		Dialect dialect = new StandardDialectResolver().resolveDialect( info );

		StringBuilder builder = new StringBuilder( productName ).append( " " )
				.append( majorVersion );
		if ( minorVersion > 0 ) {
			builder.append( "." ).append( minorVersion );
		}
		String dbms = builder.toString();

		assertNotNull( "Dialect for " + dbms + " should not be null", dialect );
		// Make sure to test that the actual dialect class is as expected
		// (not just an instance of the expected dialect.
		assertEquals( "Dialect for " + dbms + " should be " + expectedDialect.getSimpleName(),
					  expectedDialect,
					  dialect.getClass()
		);
	}
}
