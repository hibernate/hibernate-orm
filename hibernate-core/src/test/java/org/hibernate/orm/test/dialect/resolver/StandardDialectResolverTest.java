/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.resolver;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test of the {@link StandardDialectResolver} class.
 *
 * @author Bryan Turner
 */
public class StandardDialectResolverTest extends BaseUnitTestCase {

	@Test
	public void testResolveDialectInternalForSQLServer2000() {
		runSQLServerDialectTest( 8 );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2005() {
		runSQLServerDialectTest( 9 );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2008() {
		runSQLServerDialectTest( 10 );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2012() {
		runSQLServerDialectTest( 11 );
	}

	@Test
	public void testResolveDialectInternalForSQLServer2014() {
		runSQLServerDialectTest( 12 );
	}

	@Test
	public void testResolveDialectInternalForUnknownSQLServerVersion() {
		runSQLServerDialectTest( 7 );
	}

	@Test
	public void testResolveDialectInternalForPostgres81() {
		runPostgresDialectTest( 8, 1 );
	}

	@Test
	public void testResolveDialectInternalForPostgres82() {
		runPostgresDialectTest( 8, 2 );
	}

	@Test
	public void testResolveDialectInternalForPostgres83() {
		runPostgresDialectTest( 8, 3 );
	}

	@Test
	public void testResolveDialectInternalForPostgres84() {
		runPostgresDialectTest( 8, 4 );
	}

	@Test
	public void testResolveDialectInternalForPostgres9() {
		runPostgresDialectTest( 9, 0 );
	}

	@Test
	public void testResolveDialectInternalForPostgres91() {
		runPostgresDialectTest( 9, 1 );
	}

	@Test
	public void testResolveDialectInternalForPostgres92() {
		runPostgresDialectTest( 9, 2 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB103() {
		runMariaDBDialectTest( 10, 3 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB102() {
		runMariaDBDialectTest( 10, 2 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB101() {
		runMariaDBDialectTest( 10, 1 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB100() {
		runMariaDBDialectTest( 10, 0 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB55() {
		runMariaDBDialectTest( 5, 5 );
	}

	@Test
	public void testResolveDialectInternalForMariaDB52() {
		runMariaDBDialectTest( 5, 2 );
	}

	@Test
	public void testResolveDialectInternalForMySQL57() {
		runMySQLDialectTest( 5, 7 );
	}

	@Test
	public void testResolveDialectInternalForMySQL6() {
		runMySQLDialectTest( 6, 0 );
	}

	@Test
	public void testResolveDialectInternalForMySQL7() {
		runMySQLDialectTest( 7, 0 );
	}


	@Test
	public void testResolveDialectInternalForMySQL8() {
		runMySQLDialectTest( 8, 0 );
	}

	private static void runMariaDBDialectTest(int majorVersion, int minorVersion) {
		runDialectTest( "MariaDB", "MariaDB connector/J", majorVersion, minorVersion, MariaDBDialect.class );
	}

	private static void runMySQLDialectTest(int majorVersion, int minorVersion) {
		runDialectTest( "MySQL", "MySQL connector/J", majorVersion, minorVersion, MySQLDialect.class );
	}

	private static void runSQLServerDialectTest(int version) {
		runDialectTest( "Microsoft SQL Server", version, 0, SQLServerDialect.class );
	}

	private static void runPostgresDialectTest(
			int majorVersion,
			int minorVersion) {
		runDialectTest( "PostgreSQL", majorVersion, minorVersion, PostgreSQLDialect.class );
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
