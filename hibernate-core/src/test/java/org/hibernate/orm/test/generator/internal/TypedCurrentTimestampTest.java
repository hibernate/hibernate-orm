package org.hibernate.orm.test.generator.internal;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import org.hibernate.HibernateException;
import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.internal.TypedCurrentTimestamp;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/// Instrumented JDBC controls supplement live timestamp-version batching tests.
///
/// @author Steve Ebersole
class TypedCurrentTimestampTest {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void queryPreparationAndTypedExtraction(boolean callable) throws SQLException {
		var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		var coordinator = session.getJdbcCoordinator();
		PreparedStatement statement = callable ? mock( CallableStatement.class ) : mock( PreparedStatement.class );
		var jdbcType = mock( JdbcType.class );
		@SuppressWarnings("unchecked")
		ValueExtractor<LocalDateTime> extractor = mock( ValueExtractor.class );
		when( jdbcType.getExtractor( LocalDateTimeJavaType.INSTANCE ) ).thenReturn( extractor );
		when( coordinator.getStatementPreparer().prepareQueryStatement( "clock", callable ) ).thenReturn( statement );
		var value = LocalDateTime.of( 2026, 11, 10, 12, 0 );
		if ( callable ) {
			when( extractor.extract( (CallableStatement) statement, 1, session ) ).thenReturn( value );
		}
		else {
			var result = mock( ResultSet.class );
			when( coordinator.getResultSetReturn().extract( statement, "clock" ) ).thenReturn( result );
			when( result.next() ).thenReturn( true );
			when( extractor.extract( result, 1, session ) ).thenReturn( value );
		}
		assertEquals( value, TypedCurrentTimestamp.generate( session, LocalDateTimeJavaType.INSTANCE,
				jdbcType, new CurrentTimestampSelection( "clock", callable ) ) );
		if ( callable ) verify( jdbcType ).registerOutParameter( (CallableStatement) statement, 1 );
		verify( coordinator.getStatementPreparer(), never() ).prepareStatement( anyString(), anyBoolean() );
		verify( coordinator, never() ).executeBatch();
		verify( coordinator.getLogicalConnection().getResourceRegistry() ).release( statement );
		verify( coordinator ).afterStatementExecution();
	}

	@Test
	void callableExtractionFailureReleasesTheStatement() throws SQLException {
		var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		var coordinator = session.getJdbcCoordinator();
		var statement = mock( CallableStatement.class );
		var jdbcType = mock( JdbcType.class );
		@SuppressWarnings("unchecked")
		ValueExtractor<LocalDateTime> extractor = mock( ValueExtractor.class );
		when( jdbcType.getExtractor( LocalDateTimeJavaType.INSTANCE ) ).thenReturn( extractor );
		when( coordinator.getStatementPreparer().prepareQueryStatement( "clock", true ) ).thenReturn( statement );
		var failure = new SQLException( "clock unavailable" );
		when( extractor.extract( statement, 1, session ) ).thenThrow( failure );
		when( session.getJdbcServices().getSqlExceptionHelper().convert(
				failure, "Could not obtain preserving database timestamp", "clock" ) )
				.thenReturn( new GenericJDBCException( "clock unavailable", failure ) );
		assertThrows( GenericJDBCException.class, () -> TypedCurrentTimestamp.generate( session,
				LocalDateTimeJavaType.INSTANCE, jdbcType, CurrentTimestampSelection.callable( "clock" ) ) );
		verify( jdbcType ).registerOutParameter( statement, 1 );
		verify( coordinator.getLogicalConnection().getResourceRegistry() ).release( statement );
		verify( coordinator ).afterStatementExecution();
		verify( coordinator, never() ).executeBatch();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void releasesStatementOnEmptyResultOrSqlFailure(boolean sqlFailure) throws SQLException {
		var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		var coordinator = session.getJdbcCoordinator();
		var statement = mock( PreparedStatement.class );
		when( coordinator.getStatementPreparer().prepareQueryStatement( "clock", false ) ).thenReturn( statement );
		var result = mock( ResultSet.class );
		when( coordinator.getResultSetReturn().extract( statement, "clock" ) ).thenReturn( result );
		if ( sqlFailure ) {
			var failure = new SQLException( "clock unavailable" );
			when( result.next() ).thenThrow( failure );
			when( session.getJdbcServices().getSqlExceptionHelper().convert(
					failure, "Could not obtain preserving database timestamp", "clock" ) )
					.thenReturn( new GenericJDBCException( "clock unavailable", failure ) );
		}
		assertThrows( HibernateException.class, () -> TypedCurrentTimestamp.generate( session,
				LocalDateTimeJavaType.INSTANCE, mock( JdbcType.class ), CurrentTimestampSelection.prepared( "clock" ) ) );
		verify( coordinator.getLogicalConnection().getResourceRegistry() ).release( statement );
		verify( coordinator ).afterStatementExecution();
		verify( coordinator, never() ).executeBatch();
	}
}
