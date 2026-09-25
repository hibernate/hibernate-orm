package org.hibernate.community.dialect;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.lock.spi.Operation.*;
import static org.hibernate.dialect.lock.spi.BlockingDuration.*;
import static org.mockito.Mockito.*;

/// Checks that community current-read strategies use resolved concurrency facts.
///
/// @since 8.0
/// @author Steve Ebersole
class TransactionConcurrencyRoutingTest {
	@Test
	void explicitCurrentReadsRemainAvailable() {
		for ( Dialect dialect : new Dialect[] { new InformixDialect(), new FirebirdDialect(),
				new GaussDBDialect(), new AltibaseDialect(), new CUBRIDDialect(), new TimesTenDialect(),
				new PostgreSQLLegacyDialect(), new H2LegacyDialect(), new SQLServerLegacyDialect() } ) {
			final var result = resolve( dialect, null, null, JdbcMetadataOnBoot.DISALLOW );
			assertThat( result.supports( CURRENT_READ ) ).as( dialect.getClass().getSimpleName() ).isTrue();
			assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
			final var support = dialect.getLockingSupport();
			assertThat( support.renderCurrentReadClause( result ) + support.renderCurrentReadTableHint( "t", result ) )
					.isNotBlank();
		}
	}

	@Test
	void informixFreshnessDoesNotImplyRetainedLocks() {
		final var result = resolve( new InformixDialect(), null, null, JdbcMetadataOnBoot.DISALLOW );
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( UPDATE_LOCK_READ ).preventsConcurrentModification() ).isFalse();
		assertThat( result.getBlockingDuration( CURRENT_READ, WRITE ) ).isEqualTo( CONDITIONAL );
	}

	@Test
	void tidbUsesObservedTransactionMode() throws Exception {
		final var connection = modeConnection( "pessimistic" );
		final var result = resolve( new TiDBDialect(), connection, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( UPDATE_LOCK_READ ).preventsConcurrentModification() ).isTrue();
		assertThat( resolve( new TiDBDialect(), modeConnection( "optimistic" ), null, JdbcMetadataOnBoot.ALLOW )
				.supports( CURRENT_READ ) ).isFalse();
		assertThat( resolve( new TiDBDialect(), null, null, JdbcMetadataOnBoot.DISALLOW )
				.supports( CURRENT_READ ) ).isFalse();
		verify( connection, never() ).setTransactionIsolation( anyInt() );
	}

	@Test
	void tidbWriteExclusionRequiresAnObservedMode() throws Exception {
		final var dialect = new TiDBDialect();
		assertThat( resolve( dialect, modeConnection( "pessimistic" ), null, JdbcMetadataOnBoot.ALLOW )
				.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( TRANSACTION );
		for ( String mode : new String[] { "optimistic", "" } ) {
			assertThat( resolve( dialect, modeConnection( mode ), null, JdbcMetadataOnBoot.ALLOW )
					.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( NONE );
		}
		assertThat( resolve( dialect, null, null, JdbcMetadataOnBoot.DISALLOW )
				.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( UNKNOWN );
		final var connection = mock( Connection.class );
		when( connection.createStatement() ).thenThrow( new SQLException( "unavailable" ) );
		assertThat( resolve( dialect, connection, null, JdbcMetadataOnBoot.ALLOW )
				.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( UNKNOWN );
	}

	@Test
	void tidbProbeRespectsFailurePolicyAndDescriptorOverride() throws Exception {
		final var connection = mock( Connection.class );
		when( connection.createStatement() ).thenThrow( new SQLException( "probe denied" ) );
		assertThat( resolve( new TiDBDialect(), connection, null, JdbcMetadataOnBoot.ALLOW )
				.supports( CURRENT_READ ) ).isFalse();
		assertThatThrownBy( () -> resolve( new TiDBDialect(), connection, null, JdbcMetadataOnBoot.REQUIRE ) )
				.isInstanceOf( TransactionConcurrencyResolutionException.class );
		clearInvocations( connection );
		final var declaration = TransactionConcurrencies.builder( "explicit override" ).build();
		assertThat( resolve( new TiDBDialect(), connection, declaration, JdbcMetadataOnBoot.REQUIRE ) ).isSameAs( declaration );
		verifyNoInteractions( connection );
	}

	private static TransactionConcurrency resolve(Dialect dialect, Connection connection, Object declaration, JdbcMetadataOnBoot access) {
		final var metadata = mock( JdbcMetadata.class );
		when( metadata.isJdbcMetadataAccessible() ).thenReturn( true );
		when( metadata.getTransactionIsolation() ).thenReturn( Connection.TRANSACTION_READ_COMMITTED );
		return dialect.getLockingSupport().getTransactionConcurrencyResolver().resolve( dialect, metadata, connection, declaration, access );
	}

	private static Connection modeConnection(String mode) throws SQLException {
		final var connection = mock( Connection.class );
		final var statement = mock( Statement.class );
		final var rows = mock( ResultSet.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( "select @@tidb_txn_mode" ) ).thenReturn( rows );
		when( rows.next() ).thenReturn( true );
		when( rows.getString( 1 ) ).thenReturn( mode );
		return connection;
	}
}
