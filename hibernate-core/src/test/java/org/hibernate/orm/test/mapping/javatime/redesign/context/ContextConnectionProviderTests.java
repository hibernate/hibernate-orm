package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Deterministic ownership checks for the test harness, including exceptional exits.
/// No database or driver is involved in these connection counts.
///
/// @author Steve Ebersole
class ContextConnectionProviderTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void reuseIsOptInAndNeverSharesAnActiveLoan(boolean reuse) throws SQLException {
		final var opened = new ArrayList<Connection>();
		final var provider = new ContextConnectionProvider( false, settings -> {
			final var connection = mock( Connection.class );
			opened.add( connection );
			return connection;
		} );
		provider.configure( Map.of( "hibernate.test.temporal.reuse_connection", reuse ) );
		final var first = provider.getConnection();
		final var concurrent = provider.getConnection();
		assertThat( concurrent ).isNotSameAs( first );
		provider.closeConnection( first );
		provider.closeConnection( concurrent );
		final var next = provider.getConnection();
		if ( reuse ) assertThat( next ).isSameAs( first );
		else assertThat( next ).isNotSameAs( first ).isNotSameAs( concurrent );
		provider.closeConnection( next );
		provider.stop();
		provider.stop();
		assertThat( opened ).hasSize( reuse ? 2 : 3 );
		for ( var connection : opened ) verify( connection ).close();
	}

	@Test
	void blockedBootstrapDoesNotBorrowIdleConnection() throws SQLException {
		final var connection = mock( Connection.class );
		final var count = new AtomicInteger();
		final var provider = new ContextConnectionProvider( false, settings -> {
			count.incrementAndGet(); return connection;
		} );
		provider.configure( Map.of( "hibernate.test.temporal.reuse_connection", true ) );
		provider.closeConnection( provider.getConnection() );
		provider.blocked = true;
		assertThatThrownBy( provider::getConnection ).isInstanceOf( SQLException.class ).hasMessageContaining( "bootstrap" );
		assertThat( count ).hasValue( 1 );
		provider.stop();
		verify( connection ).close();
	}

	@Test
	void sessionInitializationIsReappliedOnReuse() throws SQLException {
		final var connection = mock( Connection.class );
		final var statement = mock( Statement.class );
		when( connection.createStatement() ).thenReturn( statement );
		final var provider = new ContextConnectionProvider( false, settings -> connection );
		provider.configure( Map.of( "hibernate.test.temporal.reuse_connection", true,
				"hibernate.connection.init_sql", "set time zone 'UTC'" ) );
		provider.closeConnection( provider.getConnection() );
		provider.closeConnection( provider.getConnection() );
		provider.stop();
		verify( statement, times( 2 ) ).execute( "set time zone 'UTC'" );
		verify( connection, times( 2 ) ).commit();
		verify( connection, times( 2 ) ).rollback();
		verify( connection ).close();
	}

	@ParameterizedTest
	@ValueSource(strings = { "catalog", "autocommit", "init", "rollback" })
	void failedSetupOrReturnClosesPhysicalConnection(String stage) throws SQLException {
		final var connection = mock( Connection.class );
		final var failure = new SQLException( stage );
		final var provider = new ContextConnectionProvider( false, settings -> connection );
		provider.configure( Map.of( "hibernate.test.temporal.reuse_connection", true,
				"hibernate.test.temporal.expected_catalog", "fixture", "hibernate.connection.init_sql", "init" ) );
		when( connection.getCatalog() ).thenReturn( "fixture" );
		final var statement = mock( Statement.class );
		when( connection.createStatement() ).thenReturn( statement );
		switch ( stage ) {
			case "catalog" -> when( connection.getCatalog() ).thenThrow( failure );
			case "autocommit" -> doThrow( failure ).when( connection ).setAutoCommit( false );
			case "init" -> when( statement.execute( "init" ) ).thenThrow( failure );
			case "rollback" -> doThrow( failure ).when( connection ).rollback();
		}
		assertThatThrownBy( () -> provider.closeConnection( provider.getConnection() ) ).isSameAs( failure );
		provider.stop();
		verify( connection ).close();
	}

	@Test
	void disabledBootstrapNeverOpensPhysicalConnection() {
		final var provider = new ContextConnectionProvider( true, settings -> { throw new AssertionError( "opened" ); } );
		provider.configure( Map.of() );
		assertThatThrownBy( provider::getConnection ).isInstanceOf( SQLException.class );
		assertThat( provider.attempts ).isEqualTo( 1 );
		provider.stop();
	}

	@Test
	void cleanupClosesAllOwnersEvenAfterFailure() {
		final var closed = new ArrayList<String>();
		final var first = new IllegalStateException( "drop failed" );
		final var second = new IllegalStateException( "factory close failed" );
		assertThatThrownBy( () -> ContextScope.cleanup(
				() -> { throw first; },
				() -> { closed.add( "factory" ); throw second; },
				() -> closed.add( "registry" ),
				() -> closed.add( "provision" )
		) ).isSameAs( first );
		assertThat( first.getSuppressed() ).containsExactly( second );
		assertThat( closed ).containsExactly( "factory", "registry", "provision" );
	}
}
