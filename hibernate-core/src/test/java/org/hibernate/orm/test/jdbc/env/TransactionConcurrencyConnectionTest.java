/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.lock.spi.BlockingDuration.TRANSACTION;
import static org.hibernate.dialect.lock.spi.Operation.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Exercises visibility and retained protection using independent physical
/// connections, including a write committed after the reader's snapshot.
///
/// @since 8.0
/// @author Steve Ebersole
class TransactionConcurrencyConnectionTest {
	@ParameterizedTest
	@ValueSource(ints = { 2, 4, 6, 8 })
	void snapshotStabilityAndCurrentReadAreDistinct(int isolation) throws Exception {
		final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";LOCK_TIMEOUT=100";
		try ( var reader = DriverManager.getConnection( url ); var writer = DriverManager.getConnection( url ) ) {
			initialize( reader );
			reader.setTransactionIsolation( isolation );
			reader.setAutoCommit( false );
			final var concurrency = resolve( reader );
			assertThat( read( reader, false ) ).isZero();
			try ( var update = writer.createStatement() ) {
				assertThat( update.executeUpdate( "update sample set revision = 1 where id = 1" ) ).isOne();
			}
			assertThat( read( reader, false ) ).isEqualTo(
					concurrency.getReadGuarantees( READ ).hasStableRowView() ? 0 : 1 );
			assertThat( concurrency.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
			try {
				// A successful explicit current read must never validate the stale version.
				assertThat( read( reader, true ) ).isOne();
			}
			catch (SQLException conflict) {
				assertThat( conflict.getSQLState() ).isEqualTo( "40001" );
			}
			finally {
				reader.rollback();
			}
		}
	}

	@Test
	void updateReadRetainsProtectionAfterStatementCompletion() throws Exception {
		final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";LOCK_TIMEOUT=100";
		try ( var reader = DriverManager.getConnection( url ); var writer = DriverManager.getConnection( url ) ) {
			initialize( reader );
			reader.setAutoCommit( false );
			writer.setAutoCommit( false );
			final var concurrency = resolve( reader );
			assertThat( concurrency.getBlockingDuration( UPDATE_LOCK_READ, WRITE ) ).isEqualTo( TRANSACTION );
			assertThat( concurrency.getReadGuarantees( UPDATE_LOCK_READ ).preventsConcurrentModification() ).isTrue();
			assertThat( read( reader, true ) ).isZero();
			try ( var update = writer.createStatement() ) {
				assertThatThrownBy( () -> update.executeUpdate( "update sample set revision = 1 where id = 1" ) )
						.isInstanceOfSatisfying( SQLException.class, ex -> assertThat( ex.getErrorCode() ).isEqualTo( 50200 ) );
				writer.rollback();
				reader.commit();
				assertThat( update.executeUpdate( "update sample set revision = 1 where id = 1" ) ).isOne();
				writer.commit();
			}
		}
	}

	private static void initialize(Connection connection) throws SQLException {
		try ( var statement = connection.createStatement() ) {
			statement.executeUpdate( "create table sample (id integer primary key, revision integer)" );
			statement.executeUpdate( "insert into sample values (1, 0)" );
		}
	}

	private static int read(Connection connection, boolean lock) throws SQLException {
		try ( var statement = connection.createStatement();
				var result = statement.executeQuery( "select revision from sample where id = 1" + (lock ? " for update" : "") ) ) {
			assertThat( result.next() ).isTrue();
			return result.getInt( 1 );
		}
	}

	private static TransactionConcurrency resolve(Connection connection) throws SQLException {
		final var metadata = mock( JdbcMetadata.class );
		when( metadata.isJdbcMetadataAccessible() ).thenReturn( true );
		when( metadata.getTransactionIsolation() ).thenReturn( connection.getTransactionIsolation() );
		final var dialect = new H2Dialect();
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				.resolve( dialect, metadata, connection, null, JdbcMetadataOnBoot.ALLOW );
	}
}
