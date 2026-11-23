package org.hibernate.orm.test.dialect.unit.locking;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.Timeouts;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies ASE server-option detection through the dialect bootstrap constructor.
///
/// @author Steve Ebersole
@BaseUnitTest
class SybaseSelectForUpdateDetectionTest {
	static Stream<Arguments> bootstrapCases() {
		return Stream.of( "jConnect (TM) for JDBC (TM)", "jTDS Type 4 JDBC Driver for MS SQL Server and Sybase" )
				.flatMap( driver -> Stream.of( Detection.values() ).map( detection -> Arguments.of( driver, detection ) ) );
	}

	@ParameterizedTest
	@MethodSource("bootstrapCases")
	void detectsServerOption(String driver, Detection detection) throws SQLException {
		var info = mock( DialectResolutionInfo.class );
		when( info.getDriverName() ).thenReturn( driver );
		when( info.getDatabaseVersion() ).thenReturn( "16.0" );
		when( info.getDatabaseMajorVersion() ).thenReturn( 16 );
		if ( detection != Detection.NO_METADATA ) {
			var metadata = mock( DatabaseMetaData.class );
			var connection = mock( Connection.class );
			var statement = mock( Statement.class );
			when( info.getDatabaseMetadata() ).thenReturn( metadata );
			when( metadata.getConnection() ).thenReturn( connection );
			when( connection.createStatement() ).thenReturn( statement );
			when( statement.executeQuery( anyString() ) ).thenAnswer( invocation -> {
				String sql = invocation.getArgument( 0 );
				var result = mock( ResultSet.class );
				if ( sql.contains( "syscurconfigs" ) ) {
					if ( detection == Detection.ERROR ) {
						throw new SQLException( "Option metadata unavailable" );
					}
					when( result.next() ).thenReturn( detection != Detection.EMPTY );
					when( result.getInt( 1 ) ).thenReturn( detection == Detection.ENABLED ? 1 : 0 );
				}
				else {
					when( result.next() ).thenReturn( true );
					when( result.getBytes( 1 ) ).thenReturn( new byte[5] );
					when( result.getInt( 1 ) ).thenReturn( 16384 );
				}
				return result;
			} );
		}
		var support = new SybaseASEDialect( info ).getLockingSupport();
		var request = new LockingClauseRequest( PessimisticLockKind.UPDATE, Timeouts.SKIP_LOCKED, List.of() );
		if ( detection == Detection.ENABLED || detection == Detection.NO_METADATA ) {
			assertThat( support.getLockingClauseRenderer().render( request ) ).isEqualTo( " for update" );
		}
		else {
			assertThatThrownBy( () -> support.getLockingClauseRenderer().render( request ) )
					.isInstanceOf( IllegalQueryOperationException.class )
					.hasMessageContaining( "requires the server option 'select for update'" );
		}
		assertThat( support.getLockingClauseRenderer().render(
				new LockingClauseRequest( PessimisticLockKind.SHARE, Timeouts.SKIP_LOCKED, List.of() ) ) )
				.isEqualTo( " at isolation 2" );
	}

	enum Detection { ENABLED, DISABLED, EMPTY, ERROR, NO_METADATA }
}
