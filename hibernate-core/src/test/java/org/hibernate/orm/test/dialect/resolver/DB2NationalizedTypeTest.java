package org.hibernate.orm.test.dialect.resolver;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.Database;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.hibernate.testing.orm.junit.JiraKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static java.sql.Types.CHAR;
import static java.sql.Types.CLOB;
import static java.sql.Types.INTEGER;
import static java.sql.Types.LONGNVARCHAR;
import static java.sql.Types.LONGVARCHAR;
import static java.sql.Types.NCHAR;
import static java.sql.Types.NCLOB;
import static java.sql.Types.NVARCHAR;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.VARCHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@JiraKey("HHH-21137")
class DB2NationalizedTypeTest {
	private final DatabaseMetaData metadata = mock();
	private final Connection connection = mock();
	private final Statement statement = mock();

	private Dialect resolve(String version) throws SQLException {
		when( metadata.getDatabaseProductVersion() ).thenReturn( version );
		return Database.DB2.createDialect( new DatabaseMetaDataDialectResolutionInfoAdapter( metadata ) );
	}

	private Dialect luw(String codeset, String mapping) throws SQLException {
		when( metadata.getConnection() ).thenReturn( connection );
		when( connection.createStatement() ).thenReturn( statement );
		final ResultSet rows = mock();
		when( statement.executeQuery( anyString() ) ).thenReturn( rows );
		when( rows.next() ).thenReturn( true, true, false );
		when( rows.getString( 1 ) ).thenReturn( "nchar_mapping", "codeset" );
		when( rows.getString( 2 ) ).thenReturn( mapping, codeset );
		final var dialect = resolve( "SQL120140" );
		verify( rows ).close();
		verify( statement ).close();
		verify( connection, never() ).close();
		return dialect;
	}

	@Test
	void characterMapping() throws SQLException {
		final var dialect = luw( "utf-8", "CHAR_CU32" );
		assertCharacterPairs( dialect, true );
		assertFalse( dialect.equivalentTypes( NCLOB, CLOB, "DBCLOB" ) );
		assertFalse( dialect.equivalentTypes( CLOB, CLOB, "DBCLOB" ) );
		assertTrue( dialect.equivalentTypes( CLOB, CLOB, "CLOB" ) );
		assertFalse( dialect.equivalentTypes( NCHAR, NCHAR, "GRAPHIC" ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "GRAPHIC_CU16", "GRAPHIC_CU32" })
	void graphicMapping(String mapping) throws SQLException {
		final var dialect = luw( "UTF-8", mapping );
		assertCharacterPairs( dialect, false );
		// DatabaseMetaData and ResultSetMetaData may return different codes for the same column.
		assertTrue( dialect.equivalentTypes( NCHAR, CHAR, "GRAPHIC" ) );
		assertTrue( dialect.equivalentTypes( NVARCHAR, VARCHAR, "VARGRAPHIC" ) );
		assertTrue( dialect.equivalentTypes( NCLOB, CLOB, "DBCLOB" ) );
		assertTrue( dialect.equivalentTypes( NCLOB, NCLOB, "DBCLOB" ) );
		assertFalse( dialect.equivalentTypes( CHAR, CHAR, "GRAPHIC" ) );
		assertFalse( dialect.equivalentTypes( NCLOB, CLOB, "CLOB" ) );
		assertFalse( dialect.equivalentTypes( NCHAR, VARCHAR, "VARGRAPHIC" ) );
	}

	@ParameterizedTest
	@CsvSource({ "ISO8859-1, NOT APPLICABLE", "ISO8859-1, CHAR_CU32", "utf-8, NOT APPLICABLE" })
	void unsupported(String codeset, String mapping) throws SQLException {
		final var dialect = luw( codeset, mapping );
		assertCharacterPairs( dialect, false );
	}

	@ParameterizedTest
	@CsvSource({ "utf-8,", ",CHAR_CU32", "utf-8,UNKNOWN" })
	void incompleteConfiguration(String codeset, String mapping) throws SQLException {
		assertCharacterPairs( luw( codeset, mapping ), true );
	}

	@Test
	void unavailableConfiguration() throws SQLException {
		assertCharacterPairs( new DB2Dialect(), true );
		assertCharacterPairs( new DB2Dialect( TestingDialectResolutionInfo.forDatabaseInfo( "DB2", 12, 1 ) ), true );

		when( metadata.getConnection() ).thenReturn( connection );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( anyString() ) ).thenThrow( new SQLException( "Access denied", "42501" ) );
		assertCharacterPairs( resolve( "SQL120140" ), true );
		verify( statement ).close();
	}

	@Test
	void otherPlatforms() throws SQLException {
		final var db2i = resolve( "QSQ070500" );
		final var db2z = resolve( "DSN120100" );
		assertEquals( DB2iDialect.class, db2i.getClass() );
		assertEquals( DB2zDialect.class, db2z.getClass() );
		for ( var dialect : List.of( db2i, db2z ) ) {
			assertCharacterPairs( dialect, true );
			assertTrue( dialect.equivalentTypes( CHAR, CHAR, "GRAPHIC" ) );
		}
		verify( metadata, never() ).getConnection();
	}

	private static void assertCharacterPairs(Dialect dialect, boolean equivalent) {
		assertEquals( equivalent, dialect.equivalentTypes( NCHAR, CHAR ) );
		assertEquals( equivalent, dialect.equivalentTypes( CHAR, NCHAR ) );
		assertEquals( equivalent, dialect.equivalentTypes( NVARCHAR, VARCHAR ) );
		assertEquals( equivalent, dialect.equivalentTypes( VARCHAR, NVARCHAR ) );
		assertEquals( equivalent, dialect.equivalentTypes( NCLOB, CLOB ) );
		assertEquals( equivalent, dialect.equivalentTypes( CLOB, NCLOB ) );
		assertEquals( equivalent, dialect.equivalentTypes( LONGNVARCHAR, VARCHAR ) );
		assertTrue( dialect.equivalentTypes( LONGNVARCHAR, NVARCHAR ) );
		assertTrue( dialect.equivalentTypes( VARCHAR, LONGVARCHAR ) );
		assertTrue( dialect.equivalentTypes( SMALLINT, INTEGER ) );
		assertFalse( dialect.equivalentTypes( INTEGER, SMALLINT ) );
		assertFalse( dialect.equivalentTypes( NCHAR, CLOB ) );
	}
}
