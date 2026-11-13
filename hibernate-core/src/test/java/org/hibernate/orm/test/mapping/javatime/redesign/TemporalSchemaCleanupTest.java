package org.hibernate.orm.test.mapping.javatime.redesign;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/// Absent Oracle objects are cleanup conditions; syntax and ownership errors are failures.
///
/// @author Steve Ebersole
class TemporalSchemaCleanupTest {
	@Test
	void missingOracleTableIsIdempotentButOtherFailuresPropagate() throws SQLException {
		var connection = mock( Connection.class );
		var metadata = mock( DatabaseMetaData.class );
		var statement = mock( Statement.class );
		when( connection.getMetaData() ).thenReturn( metadata );
		when( metadata.getDatabaseProductName() ).thenReturn( "Oracle" );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.execute( "drop table owned_fixture" ) )
				.thenThrow( new SQLException( "table does not exist", "42000", 942 ) )
				.thenThrow( new SQLException( "invalid SQL", "42000", 933 ) );
		TemporalSchemaCleanup.dropStatement( connection, "drop table owned_fixture" );
		assertThrows( SQLException.class,
				() -> TemporalSchemaCleanup.dropStatement( connection, "drop table owned_fixture" ) );
		verify( statement, times( 2 ) ).close();
	}

	@Test
	void postgresUsesIfExistsWithoutSwallowingErrors() throws SQLException {
		var connection = mock( Connection.class );
		var metadata = mock( DatabaseMetaData.class );
		var statement = mock( Statement.class );
		when( connection.getMetaData() ).thenReturn( metadata );
		when( metadata.getDatabaseProductName() ).thenReturn( "PostgreSQL" );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.execute( "drop table if exists owned_fixture" ) )
				.thenThrow( new SQLException( "permission denied", "42501" ) );
		assertThrows( SQLException.class,
				() -> TemporalSchemaCleanup.dropStatement( connection, "drop table owned_fixture" ) );
		verify( statement ).close();
	}
}
