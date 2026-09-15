/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jdbc.env;



import org.hibernate.dialect.DB2Dialect;

import org.hibernate.action.internal.EntityVerifyVersionProcess;

import org.hibernate.engine.spi.EntityEntry;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;

import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;

import java.sql.DatabaseMetaData;

import org.hibernate.dialect.lock.spi.TransactionConcurrencyResolutionException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.TransactionSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies;
import org.hibernate.dialect.lock.spi.TransactionConcurrencies.Guarantees;
import org.hibernate.dialect.lock.spi.TransactionConcurrency;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.env.JdbcMetadataOnBoot;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.SimpleSelect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.dialect.lock.spi.BlockingDuration.*;
import static org.hibernate.dialect.lock.spi.Operation.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/// Verifies observed, declared, and unavailable concurrency facts independently
/// of the isolation chosen for the test suite's database.
///
/// @since 8.0
/// @author Steve Ebersole
class TransactionConcurrencyTest {
	@Test
	void sqlServerReadCommittedVariantsAffectFactorySql() throws Exception {
		final var dialect = new SQLServerDialect();
		final var locking = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), snapshotConnection( false ), null,
				JdbcMetadataOnBoot.ALLOW );
		final var snapshot = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), snapshotConnection( true ), null,
				JdbcMetadataOnBoot.ALLOW );
		assertThat( locking.getName() ).isNotEqualTo( snapshot.getName() );
		assertThat( locking.getBlockingDuration( READ, WRITE ) ).isEqualTo( STATEMENT );
		assertThat( locking.getBlockingDuration( WRITE, READ ) ).isEqualTo( TRANSACTION );
		assertThat( snapshot.getBlockingDuration( WRITE, READ ) ).isEqualTo( NONE );
		assertThat( snapshot.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		assertThat( snapshot.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( snapshot.getReadGuarantees( CURRENT_READ ).holdsRowLockUntilTransactionCompletion() ).isFalse();
		assertThat( sql( dialect, locking ) ).doesNotContain( "readcommittedlock" );
		assertThat( sql( dialect, snapshot ) ).contains( "readcommittedlock" );
	}

	@Test
	void snapshotStabilityDoesNotPreventModification() {
		final var concurrency = resolve( new H2Dialect(), metadata( TRANSACTION_REPEATABLE_READ ), null, null,
				JdbcMetadataOnBoot.ALLOW );
		assertThat( concurrency.getReadGuarantees( READ ).hasStableRowView() ).isTrue();
		assertThat( concurrency.getReadGuarantees( READ ).preventsConcurrentModification() ).isFalse();
		assertThat( concurrency.getBlockingDuration( READ, WRITE ) ).isEqualTo( NONE );
		assertThat( concurrency.getReadGuarantees( UPDATE_LOCK_READ ).preventsConcurrentModification() ).isTrue();
		assertThat( concurrency.getBlockingDuration( UPDATE_LOCK_READ, WRITE ) ).isEqualTo( TRANSACTION );
	}

	@Test
	void observedConnectionIsolationWinsOverDriverDefault() {
		final var metadata = metadata( TRANSACTION_REPEATABLE_READ );
		when( metadata.getDefaultTransactionIsolation() ).thenReturn( TRANSACTION_READ_COMMITTED );
		assertThatThrownBy( () -> resolve( new H2Dialect(), metadata, null, "READ_COMMITTED", JdbcMetadataOnBoot.ALLOW ) )
				.isInstanceOf( HibernateException.class ).hasMessageContaining( "contradicts" );
	}

	@Test
	void declarationsCannotContradictSnapshotConfiguration() throws Exception {
		assertThatThrownBy( () -> resolve( new SQLServerDialect(), metadata( TRANSACTION_READ_COMMITTED ),
				snapshotConnection( true ), "LOCKING_READ_COMMITTED", JdbcMetadataOnBoot.ALLOW ) )
				.isInstanceOf( HibernateException.class ).hasMessageContaining( "snapshot configuration" );
		assertThatThrownBy( () -> resolve( new H2Dialect(), mock( JdbcMetadata.class ), null,
				"LOCKING_READ_COMMITTED", JdbcMetadataOnBoot.DISALLOW ) )
				.isInstanceOf( HibernateException.class ).hasMessageContaining( "dialect read behavior" );
	}

	@ParameterizedTest
	@EnumSource(JdbcMetadataOnBoot.class)
	void accessPolicyControlsProbeFailures(JdbcMetadataOnBoot access) throws Exception {
		final Connection connection = mock( Connection.class );
		final SQLException failure = new SQLException( "configuration denied" );
		when( connection.createStatement() ).thenThrow( failure );
		final var metadata = metadata( TRANSACTION_READ_COMMITTED );
		if ( access == JdbcMetadataOnBoot.REQUIRE ) {
			assertThatThrownBy( () -> resolve( new SQLServerDialect(), metadata, connection, null, access ) )
					.isInstanceOf( HibernateException.class ).hasCause( failure );
		}
		else {
			final var result = resolve( new SQLServerDialect(), metadata, connection, null, access );
			assertThat( result.getBlockingDuration( WRITE, READ ) ).isEqualTo( UNKNOWN );
			assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
			assertThat( metadata.getTransactionIsolation() ).isEqualTo( TRANSACTION_READ_COMMITTED );
		}
		if ( access == JdbcMetadataOnBoot.DISALLOW ) {
			verify( connection, never() ).createStatement();
		}
		verify( connection, never() ).setTransactionIsolation( anyInt() );
	}

	@Test
	void declarationFillsFailedProbeWithoutDiscardingIsolation() throws Exception {
		final Connection connection = mock( Connection.class );
		when( connection.createStatement() ).thenThrow( new SQLException( "unavailable" ) );
		final var result = resolve( new SQLServerDialect(), metadata( TRANSACTION_READ_COMMITTED ), connection,
				"READ_COMMITTED_SNAPSHOT", JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( READ ).preventsDirtyReads() ).isTrue();
		assertThat( result.getBlockingDuration( WRITE, READ ) ).isEqualTo( NONE );
	}

	@Test
	void noJdbcBootstrapUsesDeclarationWithoutAcquiringConnection() {
		final ConnectionProvider provider = mock( ConnectionProvider.class );
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, SQLServerDialect.class.getName() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, JdbcMetadataOnBoot.DISALLOW )
				.applySetting( TransactionSettings.TRANSACTION_CONCURRENCY, "READ_COMMITTED_SNAPSHOT" )
				.addService( ConnectionProvider.class, provider )
				.build() ) {
			final var environment = registry.requireService( JdbcEnvironment.class );
			assertThat( environment.getJdbcMetadata().isJdbcMetadataAccessible() ).isFalse();
			assertThat( environment.getTransactionConcurrency().getReadGuarantees( READ ).preventsDirtyReads() ).isTrue();
			assertThat( environment.getTransactionConcurrency().getBlockingDuration( WRITE, READ ) ).isEqualTo( NONE );
		}
		try {
			verify( provider, never() ).getConnection();
		}
		catch (SQLException ex) {
			throw new AssertionError( ex );
		}
	}

	@Test
	void absentMetadataDoesNotEstablishTransactionNoneOrDefaultIsolation() {
		final var metadata = mock( JdbcMetadata.class );
		final var result = resolve( new SQLServerDialect(), metadata, null, null, JdbcMetadataOnBoot.DISALLOW );
		assertThat( result.getName() ).contains( "UNKNOWN" );
		assertThat( result.getReadGuarantees( READ ).preventsDirtyReads() ).isFalse();
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		verify( metadata, never() ).getTransactionIsolation();
		verify( metadata, never() ).getDefaultTransactionIsolation();
	}

	@Test
	void explicitSharedLockSupportDoesNotIncludeUpdateSubstitution() {
		final var result = resolve( new OracleDialect(), metadata( TRANSACTION_READ_COMMITTED ), null, null,
				JdbcMetadataOnBoot.ALLOW );
		assertThat( result.supports( SHARED_LOCK_READ ) ).isFalse();
		assertThat( result.supports( CURRENT_READ ) ).isTrue();
		assertThatThrownBy( () -> result.getBlockingDuration( SHARED_LOCK_READ, WRITE ) )
				.isInstanceOf( UnsupportedOperationException.class );
		assertThatThrownBy( () -> result.getReadGuarantees( WRITE ) ).isInstanceOf( IllegalArgumentException.class );
	}

	@ParameterizedTest
	@EnumSource(JdbcMetadataOnBoot.class)
	void suppliedDescriptorIsAuthoritativeWithoutObservations(JdbcMetadataOnBoot access) {
		final var declaration = TransactionConcurrencies.builder( "application override" )
				.read( READ, new Guarantees( true, true, true, true, true ) )
				.read( CURRENT_READ, new Guarantees( true, true, true, true, true ) )
				.blocking( CURRENT_READ, WRITE, TRANSACTION )
				.build();
		final var metadata = mock( JdbcMetadata.class );
		final var connection = mock( Connection.class );
		final var result = resolve( new SQLServerDialect(), metadata, connection, declaration, access );
		assertThat( result ).isSameAs( declaration );
		verifyNoInteractions( metadata, connection );
	}

	@ParameterizedTest
	@EnumSource(JdbcMetadataOnBoot.class)
	void bootstrapBypassesEvenACustomResolverForSuppliedDescriptors(JdbcMetadataOnBoot access) {
		final var declaration = TransactionConcurrencies.builder( "application override" ).build();
		final var support = spy( new H2Dialect().getLockingSupport() );
		doThrow( new AssertionError( "Resolver must not be obtained" ) )
				.when( support ).getTransactionConcurrencyResolver();
		final var dialect = new H2Dialect() {
			@Override
			public org.hibernate.dialect.lock.spi.LockingSupport getLockingSupport() {
				return support;
			}
		};
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( JdbcSettings.DIALECT, dialect )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, access )
				.applySetting( JdbcSettings.DRIVER, "org.h2.Driver" )
				.applySetting( JdbcSettings.URL, "jdbc:h2:mem:descriptor_override_" + access )
				.applySetting( TransactionSettings.TRANSACTION_CONCURRENCY, declaration )
				.build() ) {
			assertThat( registry.requireService( JdbcEnvironment.class ).getTransactionConcurrency() )
					.isSameAs( declaration );
		}
	}

	@Test
	void cockroachSerializableDoesNotPromiseDurableLocks() {
		final var dialect = new org.hibernate.dialect.CockroachDialect();
		final var result = resolve( dialect, metadata( Connection.TRANSACTION_SERIALIZABLE ),
				null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.supports( UPDATE_LOCK_READ ) ).isTrue();
		assertThat( result.getReadGuarantees( UPDATE_LOCK_READ ).preventsConcurrentModification() ).isFalse();
		assertThat( result.getReadGuarantees( UPDATE_LOCK_READ ).holdsRowLockUntilTransactionCompletion() ).isFalse();
		assertThat( result.getBlockingDuration( UPDATE_LOCK_READ, WRITE ) ).isEqualTo( CONDITIONAL );
		assertThat( result.getBlockingDuration( UPDATE_LOCK_READ, UPDATE_LOCK_READ ) ).isEqualTo( CONDITIONAL );
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( CURRENT_READ ).preventsConcurrentModification() ).isFalse();
		assertThat( result.getBlockingDuration( CURRENT_READ, WRITE ) ).isEqualTo( CONDITIONAL );
		assertThat( result.supports( SHARED_LOCK_READ ) ).isFalse();
		assertThat( sql( dialect, result ) ).endsWith( " for update" );
	}

	@Test
	void cockroachReadCommittedHasDurableSharedAndUpdateLocks() {
		final var dialect = new org.hibernate.dialect.CockroachDialect();
		final var result = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ),
				null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.supports( SHARED_LOCK_READ ) ).isTrue();
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( UPDATE_LOCK_READ ).holdsRowLockUntilTransactionCompletion() ).isTrue();
		assertThat( result.getBlockingDuration( UPDATE_LOCK_READ, WRITE ) ).isEqualTo( TRANSACTION );
		assertThat( sql( dialect, result ) ).endsWith( " for share" );
	}

	@Test
	void fallbackDoesNotInferProtectionFromLockingSyntax() {
		final var dialect = new H2Dialect() {
			@Override
			public org.hibernate.dialect.lock.spi.LockingSupport getLockingSupport() {
				return new org.hibernate.dialect.lock.spi.LockingSupport() {
					@Override
					public org.hibernate.dialect.lock.spi.LockingClauseRenderer getLockingClauseRenderer() {
						return request -> " for update";
					}

					@Override
					public Metadata getMetadata() {
						return mock( Metadata.class );
					}

					@Override
					public org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy getConnectionLockTimeoutStrategy() {
						return org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy.NONE;
					}
				};
			}
		};
		final var result = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		assertThat( result.supports( CURRENT_READ ) ).isFalse();
		assertThat( result.supports( UPDATE_LOCK_READ ) ).isFalse();
		assertThat( result.getBlockingDuration( READ, WRITE ) ).isEqualTo( UNKNOWN );
	}

	@Test
	void namedSnapshotUsesEachDialectConfiguration() {
		final Dialect[] dialects = {
				new H2Dialect(), new SQLServerDialect(), new OracleDialect(),
				new org.hibernate.dialect.PostgreSQLDialect(), new org.hibernate.dialect.MySQLDialect(),
				new org.hibernate.dialect.MariaDBDialect()
		};
		final int[] isolationLevels = { 6, 4096, Connection.TRANSACTION_SERIALIZABLE,
				TRANSACTION_REPEATABLE_READ, TRANSACTION_REPEATABLE_READ, TRANSACTION_REPEATABLE_READ };
		for ( int i = 0; i < dialects.length; i++ ) {
			final var result = resolve( dialects[i], metadata( isolationLevels[i] ), null,
					" snapshot ", JdbcMetadataOnBoot.ALLOW );
			assertThat( result.getReadGuarantees( READ ).hasStableRowView() ).isTrue();
			assertThat( result.getReadGuarantees( READ ).preventsConcurrentModification() ).isFalse();
			assertThat( result.getBlockingDuration( READ, WRITE ) ).isEqualTo( NONE );
		}
	}

	@Test
	void mysqlSerializableUsesLockingOrdinaryReads() {
		final var result = resolve( new org.hibernate.dialect.MySQLDialect(), mock( JdbcMetadata.class ),
				null, "SERIALIZABLE", JdbcMetadataOnBoot.DISALLOW );
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( READ ).preventsConcurrentModification() ).isTrue();
		assertThat( result.getBlockingDuration( READ, WRITE ) ).isEqualTo( TRANSACTION );
	}

	@Test
	void postgresReadUncommittedStillPreventsDirtyReads() {
		final var result = resolve( new org.hibernate.dialect.PostgreSQLDialect(),
				metadata( Connection.TRANSACTION_READ_UNCOMMITTED ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( READ ).preventsDirtyReads() ).isTrue();
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
	}

	@Test
	void failedConcurrencyProbePreservesEnvironmentMetadata() throws Exception {
		final var raw = mock( DatabaseMetaData.class );
		final var connection = mock( Connection.class );
		when( raw.getConnection() ).thenReturn( connection );
		when( raw.getDatabaseProductName() ).thenReturn( "Microsoft SQL Server" );
		when( raw.getDatabaseProductVersion() ).thenReturn( "16" );
		when( raw.getDriverName() ).thenReturn( "Fixture driver" );
		when( raw.getCatalogSeparator() ).thenReturn( "." );
		when( raw.isCatalogAtStart() ).thenReturn( true );
		when( raw.supportsNamedParameters() ).thenReturn( true );
		when( connection.getTransactionIsolation() ).thenReturn( TRANSACTION_READ_COMMITTED );
		when( connection.createStatement() ).thenThrow( new SQLException( "query denied" ) );
		final var environment = new JdbcEnvironmentImpl(
				raw, new SQLServerDialect(), mock( JdbcConnectionAccess.class ) );
		assertThat( environment.getJdbcMetadata().isJdbcMetadataAccessible() ).isTrue();
		assertThat( environment.getJdbcMetadata().getDatabaseProductName() ).isEqualTo( "Microsoft SQL Server" );
		assertThat( environment.getJdbcMetadata().supportsNamedParameters() ).isTrue();
		assertThat( environment.getJdbcMetadata().getTransactionIsolation() ).isEqualTo( TRANSACTION_READ_COMMITTED );
		assertThat( environment.getTransactionConcurrency().getBlockingDuration( WRITE, READ ) ).isEqualTo( UNKNOWN );
	}

	@Test
	void unknownGuaranteeRejectsValidationBeforeExecutingVersionRead() {
		final Object entity = new Object();
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var entry = mock( EntityEntry.class );
		when( session.getPersistenceContext().getEntry( entity ) ).thenReturn( entry );
		when( session.getFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() )
				.thenReturn( TransactionConcurrencies.builder( "unresolved" ).blocking( WRITE, READ, CONDITIONAL ).build() );
		assertThatThrownBy( () -> new EntityVerifyVersionProcess( entity )
				.doBeforeTransactionCompletion( session ) )
				.isInstanceOf( HibernateException.class ).hasMessageContaining( "no strategy established" );
		verify( entry, never() ).getPersister();
	}

	@Test
	void unrecognizedVendorIsolationDoesNotEstablishReadGuarantees() {
		final var result = resolve( new H2Dialect(), metadata( 99 ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		assertThat( result.getReadGuarantees( READ ).preventsDirtyReads() ).isFalse();
	}

	@Test
	void allowBootstrapCannotHideAnObservedContradictionByFallingBack() throws Exception {
		final var provider = mock( ConnectionProvider.class );
		final var connection = snapshotConnection( true );
		final var raw = mock( DatabaseMetaData.class );
		when( provider.getConnection() ).thenReturn( connection );
		when( connection.getMetaData() ).thenReturn( raw );
		when( connection.getTransactionIsolation() ).thenReturn( TRANSACTION_READ_COMMITTED );
		when( raw.getConnection() ).thenReturn( connection );
		when( raw.getDatabaseProductName() ).thenReturn( "Microsoft SQL Server" );
		when( raw.getDatabaseProductVersion() ).thenReturn( "16.0" );
		when( raw.getDatabaseMajorVersion() ).thenReturn( 16 );
			when( raw.getDriverName() ).thenReturn( "Fixture driver" );
			when( raw.getCatalogSeparator() ).thenReturn( "." );
			when( provider.getDatabaseConnectionInfo( any( Dialect.class ),
					any( org.hibernate.engine.jdbc.env.spi.ExtractedDatabaseMetaData.class ) ) )
					.thenReturn( mock( org.hibernate.engine.jdbc.connections.spi.DatabaseConnectionInfo.class ) );
			try ( var registry = new StandardServiceRegistryBuilder()
					// Avoid the dialect constructor's separate compatibility-level query:
					// this fixture models the concurrency probe specifically.
					.applySetting( JdbcSettings.DIALECT, new SQLServerDialect() )
				.applySetting( JdbcSettings.ALLOW_METADATA_ON_BOOT, JdbcMetadataOnBoot.ALLOW )
				.applySetting( TransactionSettings.TRANSACTION_CONCURRENCY, "LOCKING_READ_COMMITTED" )
				.addService( ConnectionProvider.class, provider ).build() ) {
			assertThatThrownBy( () -> registry.requireService( JdbcEnvironment.class ) )
					.hasRootCauseInstanceOf( TransactionConcurrencyResolutionException.class )
					.hasStackTraceContaining( "snapshot configuration" );
		}
		verify( connection, never() ).setTransactionIsolation( anyInt() );
	}

	@Test
	void db2CurrentReadKeepsItsSpecializedClause() {
		final var dialect = new DB2Dialect();
		final var result = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getReadGuarantees( CURRENT_READ ).holdsRowLockUntilTransactionCompletion() ).isFalse();
		assertThat( sql( dialect, result ) ).contains( "with cs wait for outcome" );
	}

	@Test
	void dialectOnlySelectCannotBypassConcurrencyResolution() {
		assertThatThrownBy( () -> new SimpleSelect( new H2Dialect() )
				.setTableName( "Doctor" ).addColumn( "version" ).setCurrentRead( true ).toStatementString() )
				.isInstanceOf( IllegalStateException.class ).hasMessageContaining( "SessionFactory" );
	}

	@Test
	void statelessOptimisticReadsChooseRetainedProtection() {
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var retained = new Guarantees( true, true, true, true, true );
		final var shared = TransactionConcurrencies.builder( "shared" ).read( SHARED_LOCK_READ, retained ).build();
		when( session.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() ).thenReturn( shared );
		assertThat( org.hibernate.internal.StatelessLocking.getEffectiveLockMode( org.hibernate.LockMode.OPTIMISTIC, session ) )
				.isEqualTo( org.hibernate.LockMode.PESSIMISTIC_READ );
		final var update = TransactionConcurrencies.builder( "update" ).read( UPDATE_LOCK_READ, retained ).build();
		when( session.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() ).thenReturn( update );
		assertThat( org.hibernate.internal.StatelessLocking.getEffectiveLockMode( org.hibernate.LockMode.OPTIMISTIC, session ) )
				.isEqualTo( org.hibernate.LockMode.PESSIMISTIC_WRITE );
		final var shortRead = TransactionConcurrencies.builder( "fresh but unprotected" )
				.read( CURRENT_READ, new Guarantees( true, false, false, false, true ) )
				.read( READ, new Guarantees( true, true, false, false, false ) ).build();
		when( session.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() ).thenReturn( shortRead );
		assertThatThrownBy( () -> org.hibernate.internal.StatelessLocking.getEffectiveLockMode( org.hibernate.LockMode.OPTIMISTIC, session ) )
				.isInstanceOf( HibernateException.class ).hasMessageContaining( "transaction-long row protection" );
	}

	@Test
	void sybaseUsesCurrentReadsAndRetainedSharedLocks() {
		final var dialect = new org.hibernate.dialect.SybaseASEDialect();
		final var result = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isTrue();
		assertThat( result.supports( SHARED_LOCK_READ ) ).isTrue();
		assertThat( result.supports( UPDATE_LOCK_READ ) ).isFalse();
		assertThat( result.getReadGuarantees( CURRENT_READ ).isCurrentRead() ).isTrue();
		assertThat( result.getBlockingDuration( SHARED_LOCK_READ, WRITE ) ).isEqualTo( TRANSACTION );
		assertThat( sql( dialect, result ) ).doesNotContain( "holdlock" );
		assertThat( dialect.getLockingSupport().renderCurrentReadTableHint( "Doctor", result ) ).contains( "holdlock" );
		assertVersionValidationAllowed( result );
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		when( session.getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() ).thenReturn( result );
		assertThat( org.hibernate.internal.StatelessLocking.getEffectiveLockMode( org.hibernate.LockMode.OPTIMISTIC, session ) )
				.isEqualTo( org.hibernate.LockMode.PESSIMISTIC_READ );
		final var dirty = resolve( dialect, metadata( Connection.TRANSACTION_READ_UNCOMMITTED ), null, null, JdbcMetadataOnBoot.ALLOW );
		assertThat( dirty.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		assertThat( dirty.supports( CURRENT_READ ) ).isFalse();
		assertThat( dirty.supports( SHARED_LOCK_READ ) ).isFalse();
	}

	@Test
	void hsqlResolvesOrdinaryReadsWithoutInventingForUpdateLocks() throws Exception {
		final var dialect = new org.hibernate.dialect.HSQLDialect();
		for ( String mode : new String[] { "LOCKS", "MVLOCKS", "MVCC" } ) {
			final var connection = mock( Connection.class );
			final var statement = mock( Statement.class );
			final var rows = mock( ResultSet.class );
			when( connection.createStatement() ).thenReturn( statement );
			when( statement.executeQuery( anyString() ) ).thenReturn( rows );
			when( rows.next() ).thenReturn( true );
			when( rows.getString( 1 ) ).thenReturn( mode );
			final var result = resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), connection, null, JdbcMetadataOnBoot.ALLOW );
			assertThat( result.getReadGuarantees( READ ).isCurrentRead() ).isEqualTo( !mode.equals( "MVCC" ) );
			assertThat( result.supports( CURRENT_READ ) ).isFalse();
			assertThat( result.supports( UPDATE_LOCK_READ ) ).isFalse();
			assertThat( result.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( TRANSACTION );
			if ( !mode.equals( "MVCC" ) ) {
				assertVersionValidationAllowed( result );
				assertThat( sql( dialect, result ) ).doesNotContain( "for update" );
			}
			verify( rows ).close();
			verify( statement ).close();
			verify( connection, never() ).close();
			verify( connection, never() ).setTransactionIsolation( anyInt() );
			if ( mode.equals( "MVLOCKS" ) ) {
				when( connection.isReadOnly() ).thenReturn( true );
				assertThat( resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), connection, null, JdbcMetadataOnBoot.ALLOW )
						.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
			}
		}
	}

	@ParameterizedTest
	@EnumSource(JdbcMetadataOnBoot.class)
	void hsqlProbeRespectsAccessPolicy(JdbcMetadataOnBoot access) throws Exception {
		final var connection = mock( Connection.class );
		when( connection.createStatement() ).thenThrow( new SQLException( "unavailable" ) );
		final var dialect = new org.hibernate.dialect.HSQLDialect();
		if ( access == JdbcMetadataOnBoot.REQUIRE ) {
			assertThatThrownBy( () -> resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), connection, null, access ) )
					.isInstanceOf( TransactionConcurrencyResolutionException.class );
		}
		else {
			assertThat( resolve( dialect, metadata( TRANSACTION_READ_COMMITTED ), connection, null, access )
					.getReadGuarantees( READ ).isCurrentRead() ).isFalse();
		}
		if ( access == JdbcMetadataOnBoot.DISALLOW ) {
			verifyNoInteractions( connection );
		}
		final var declared = resolve( dialect, mock( JdbcMetadata.class ), null, "LOCKING_READ_COMMITTED", JdbcMetadataOnBoot.DISALLOW );
		assertThat( declared.getReadGuarantees( READ ).isCurrentRead() ).isTrue();
	}

	@Test
	void fallbackDoesNotInventWriteExclusion() {
		final var result = org.hibernate.dialect.lock.internal.StandardTransactionConcurrencyResolver.INSTANCE
				.resolve( new H2Dialect(), metadata( TRANSACTION_READ_COMMITTED ), null, null, JdbcMetadataOnBoot.DISALLOW );
		assertThat( result.getBlockingDuration( WRITE, WRITE ) ).isEqualTo( UNKNOWN );
	}

	private static void assertVersionValidationAllowed(TransactionConcurrency concurrency) {
		final var entity = new Object();
		final var session = mock( SharedSessionContractImplementor.class, RETURNS_DEEP_STUBS );
		final var entry = mock( EntityEntry.class, RETURNS_DEEP_STUBS );
		when( session.getPersistenceContext().getEntry( entity ) ).thenReturn( entry );
		when( session.getFactory().getJdbcServices().getJdbcEnvironment().getTransactionConcurrency() ).thenReturn( concurrency );
		when( entry.getVersion() ).thenReturn( 1 );
		when( entry.getPersister().getCurrentVersion( entry.getId(), session ) ).thenReturn( 1 );
		new EntityVerifyVersionProcess( entity ).doBeforeTransactionCompletion( session );
		verify( entry.getPersister() ).getCurrentVersion( entry.getId(), session );
	}

	private static TransactionConcurrency resolve(
			Dialect dialect, JdbcMetadata metadata, Connection connection, Object declaration, JdbcMetadataOnBoot access) {
		return dialect.getLockingSupport().getTransactionConcurrencyResolver()
				.resolve( dialect, metadata, connection, declaration, access );
	}

	private static JdbcMetadata metadata(int isolation) {
		final var metadata = mock( JdbcMetadata.class );
		when( metadata.isJdbcMetadataAccessible() ).thenReturn( true );
		when( metadata.getTransactionIsolation() ).thenReturn( isolation );
		return metadata;
	}

	private static Connection snapshotConnection(boolean enabled) throws SQLException {
		final var connection = mock( Connection.class );
		final var statement = mock( Statement.class );
		final var rows = mock( ResultSet.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( anyString() ) ).thenReturn( rows );
		when( rows.next() ).thenReturn( true, false );
		when( rows.getBoolean( 1 ) ).thenReturn( enabled );
		return connection;
	}

	private static String sql(Dialect dialect, TransactionConcurrency concurrency) {
		final var factory = mock( SessionFactoryImplementor.class );
		final var services = mock( JdbcServices.class );
		final var environment = mock( JdbcEnvironment.class );
		when( factory.getJdbcServices() ).thenReturn( services );
		when( services.getDialect() ).thenReturn( dialect );
		when( services.getJdbcEnvironment() ).thenReturn( environment );
		when( environment.getTransactionConcurrency() ).thenReturn( concurrency );
		return new SimpleSelect( factory ).setTableName( "Doctor" ).addColumn( "version" )
				.setCurrentRead( true ).toStatementString();
	}
}
