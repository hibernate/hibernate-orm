/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc.dialect.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.SQLServer2008Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.resolver.TestingDialectResolutionInfo;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

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
		runSQLServerDialectTest( 11, SQLServer2008Dialect.class );
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
		runPostgresDialectTest( 9, 2, PostgreSQL9Dialect.class );
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
		TestingDialectResolutionInfo info = TestingDialectResolutionInfo.forDatabaseInfo( productName, majorVersion, minorVersion );

		Dialect dialect = StandardDialectResolver.INSTANCE.resolveDialect( info );

		StringBuilder builder = new StringBuilder( productName ).append( " " )
				.append( majorVersion );
		if ( minorVersion > 0 ) {
			builder.append( "." ).append( minorVersion );
		}
		String dbms = builder.toString();

		assertNotNull( "Dialect for " + dbms + " should not be null", dialect );
		assertTrue( "Dialect for " + dbms + " should be " 
				+ expectedDialect.getSimpleName(),
						expectedDialect.isInstance( dialect ) );
	}
}
