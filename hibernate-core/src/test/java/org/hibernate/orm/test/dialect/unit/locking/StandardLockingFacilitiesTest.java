/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.unit.locking;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Timeout;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StatementObserver;
import org.hibernate.Timeouts;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.internal.CockroachLockingSupport;
import org.hibernate.dialect.lock.internal.DB2LockingSupport;
import org.hibernate.dialect.lock.internal.H2LockingSupport;
import org.hibernate.dialect.lock.internal.HANALockingSupport;
import org.hibernate.dialect.lock.internal.HSQLLockingSupport;
import org.hibernate.dialect.lock.internal.LockingSupportParameterized;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.internal.MariaDBLockingSupport;
import org.hibernate.dialect.lock.internal.MySQLLockingSupport;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.internal.OracleLockingSupport;
import org.hibernate.dialect.lock.internal.PostgreSQLLockingSupport;
import org.hibernate.dialect.lock.internal.TimesTenLockingSupport;
import org.hibernate.dialect.lock.internal.TransactSQLLockingSupport;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutOperations;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardConnectionLockTimeoutStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.spi.StringBuilderSqlAppender;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Verifies the provider-facing standard locking facilities against the
/// internal profiles they encapsulate.
///
/// @author Steve Ebersole
class StandardLockingFacilitiesTest {
	private static final LockingClauseRequest UPDATE = new LockingClauseRequest(
			PessimisticLockKind.UPDATE,
			Timeouts.NO_WAIT,
			List.of( new LockingClauseRequest.TableTarget( "t" ) )
	);

	@Test
	void genericAndNamedProfilesPreserveInternalBehavior() {
		assertProfileParity( NoLockingSupport.NO_LOCKING_SUPPORT, StandardLockingSupports.none() );
		assertProfileParity( LockingSupportSimple.STANDARD_SUPPORT, StandardLockingSupports.standard() );
		assertProfileParity(
				LockingSupportSimple.NO_OUTER_JOIN,
				StandardLockingSupports.standardWithoutOuterJoinLocking()
		);
		assertProfileParity(
				new LockingSupportSimple(
						PessimisticLockStyle.CLAUSE,
						RowLockStrategy.TABLE,
						LockTimeoutType.QUERY,
						OuterJoinLockingType.FULL,
						StandardConnectionLockTimeoutStrategies.mysql( 100_000 )
				),
				StandardLockingSupports.simple(
						PessimisticLockStyle.CLAUSE,
						RowLockStrategy.TABLE,
						LockTimeoutType.QUERY,
						OuterJoinLockingType.FULL,
						StandardConnectionLockTimeoutStrategies.mysql( 100_000 )
				)
		);
		assertProfileParity(
				new LockingSupportParameterized(
						PessimisticLockStyle.CLAUSE,
						RowLockStrategy.COLUMN,
						LockTimeoutType.QUERY,
						LockTimeoutType.NONE,
						LockTimeoutType.QUERY,
						OuterJoinLockingType.IDENTIFIED
				),
				StandardLockingSupports.parameterized(
						PessimisticLockStyle.CLAUSE,
						RowLockStrategy.COLUMN,
						LockTimeoutType.QUERY,
						LockTimeoutType.NONE,
						LockTimeoutType.QUERY,
						OuterJoinLockingType.IDENTIFIED
				)
		);

		assertProfileParity(
				DB2LockingSupport.forDB2( true ),
				StandardLockingSupports.db2( DatabaseVersion.make( 11, 5 ) )
		);
		assertProfileParity( DB2LockingSupport.forDB2i(), StandardLockingSupports.db2i() );
		assertProfileParity( DB2LockingSupport.forLegacyDB2i(), StandardLockingSupports.legacyDb2i() );
		assertProfileParity( DB2LockingSupport.forDB2z(), StandardLockingSupports.db2z() );
		assertProfileParity( H2LockingSupport.LEGACY_INSTANCE, StandardLockingSupports.h2( DatabaseVersion.make( 2, 2, 219 ) ) );
		assertProfileParity( H2LockingSupport.INSTANCE, StandardLockingSupports.h2( DatabaseVersion.make( 2, 2, 220 ) ) );
		assertProfileParity(
				HANALockingSupport.forDialectVersion( DatabaseVersion.make( 2, 0, 30 ) ),
				StandardLockingSupports.hana( DatabaseVersion.make( 2, 0, 30 ) )
		);
		assertProfileParity( HSQLLockingSupport.NO_CLAUSE_LOCKING_SUPPORT, StandardLockingSupports.hsql( DatabaseVersion.make( 1 ) ) );
		assertProfileParity( HSQLLockingSupport.LOCKING_SUPPORT, StandardLockingSupports.hsql( DatabaseVersion.make( 2 ) ) );
		assertProfileParity( new MySQLLockingSupport( DatabaseVersion.make( 8 ) ), StandardLockingSupports.mysql( DatabaseVersion.make( 8 ) ) );
		assertProfileParity( new MariaDBLockingSupport( DatabaseVersion.make( 10, 6 ) ), StandardLockingSupports.mariaDb( DatabaseVersion.make( 10, 6 ) ) );
		assertProfileParity( new OracleLockingSupport( DatabaseVersion.make( 10 ) ), StandardLockingSupports.oracle( DatabaseVersion.make( 10 ) ) );
		assertProfileParity( new PostgreSQLLockingSupport( true, true ), StandardLockingSupports.postgresql( DatabaseVersion.make( 9, 5 ) ) );
		assertProfileParity( CockroachLockingSupport.LEGACY_COCKROACH_LOCKING_SUPPORT, StandardLockingSupports.cockroach( DatabaseVersion.make( 20 ) ) );
		assertProfileParity( CockroachLockingSupport.COCKROACH_LOCKING_SUPPORT, StandardLockingSupports.cockroach( DatabaseVersion.make( 20, 1 ) ) );
		assertProfileParity( TimesTenLockingSupport.TIMES_TEN_LOCKING_SUPPORT, StandardLockingSupports.timesTen() );
		assertProfileParity( TransactSQLLockingSupport.SYBASE, StandardLockingSupports.sybase() );
		assertProfileParity( TransactSQLLockingSupport.SYBASE_ASE, StandardLockingSupports.sybaseAse() );
		assertProfileParity( TransactSQLLockingSupport.SYBASE_LEGACY, StandardLockingSupports.legacySybaseAse() );
		assertProfileParity(
				TransactSQLLockingSupport.forSybaseAnywhere( DatabaseVersion.make( 10 ) ),
				StandardLockingSupports.sybaseAnywhere( DatabaseVersion.make( 10 ) )
		);
		assertNotNull( StandardLockingSupports.sqlServer( DatabaseVersion.make( 8 ) ) );
		assertNotNull( StandardLockingSupports.sqlServer( DatabaseVersion.make( 9 ) ) );
	}

	@Test
	void namedFactoriesOwnTheirVersionBoundaries() {
		assertAll(
				() -> assertEquals(
						" for read only with rs use and keep update locks",
						StandardLockingSupports.db2( DatabaseVersion.make( 11, 4 ) )
								.getLockingClauseRenderer().render( skipLockedRequest() )
				),
				() -> assertEquals(
						" for read only with rs use and keep update locks skip locked data",
						StandardLockingSupports.db2( DatabaseVersion.make( 11, 5 ) )
								.getLockingClauseRenderer().render( skipLockedRequest() )
				),
				() -> assertEquals(
						LockTimeoutType.NONE,
						StandardLockingSupports.postgresql( DatabaseVersion.make( 8 ) )
								.getMetadata().getLockTimeoutType( Timeouts.NO_WAIT )
				),
				() -> assertEquals(
						LockTimeoutType.QUERY,
						StandardLockingSupports.postgresql( DatabaseVersion.make( 8, 1 ) )
								.getMetadata().getLockTimeoutType( Timeouts.NO_WAIT )
				),
				() -> assertEquals(
						" with (updlock,rowlock)",
						StandardLockingSupports.sqlServer( DatabaseVersion.make( 8 ) )
								.getTableLockHintRenderer().render( tableHintRequest() )
				),
				() -> assertEquals(
						" with (updlock,holdlock,rowlock,nowait)",
						StandardLockingSupports.sqlServer( DatabaseVersion.make( 9 ) )
								.getTableLockHintRenderer().render( tableHintRequest() )
				)
		);
	}

	@Test
	void clauseStrategyFactoriesProtectInputsAndPreserveRendering() {
		final var none = StandardLockingClauseStrategies.none();
		assertFalse( none.containsJoins() );
		assertFalse( none.containsOuterJoins() );
		assertTrue( none.getPathsToLock().isEmpty() );

		final NavigablePath path = new NavigablePath( "root" );
		final Set<NavigablePath> roots = new HashSet<>( Set.of( path ) );
		final var standard = StandardLockingClauseStrategies.standard(
				request -> " for provider update",
				PessimisticLockKind.UPDATE,
				RowLockStrategy.NONE,
				new LockOptions( LockMode.PESSIMISTIC_WRITE, Timeouts.WAIT_FOREVER ),
				roots
		);
		roots.clear();
		final TableGroup tableGroup = mock( TableGroup.class );
		when( tableGroup.getNavigablePath() ).thenReturn( path );
		assertTrue( standard.registerRoot( tableGroup ) );
		final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();
		standard.render( appender );
		assertEquals( " for provider update", appender.toString() );

		assertThrows(
				IllegalArgumentException.class,
				() -> StandardLockingClauseStrategies.standard(
						request -> "",
						PessimisticLockKind.NONE,
						RowLockStrategy.NONE,
						new LockOptions(),
						Set.of()
				)
		);
	}

	@Test
	void connectionOperationsUseHibernateJdbcLifecycle() throws Exception {
		final Connection connection = mock( Connection.class );
		final Statement statement = mock( Statement.class );
		final ResultSet resultSet = mock( ResultSet.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( "select provider_timeout" ) ).thenReturn( resultSet );
		when( resultSet.next() ).thenReturn( true );
		when( resultSet.getInt( 1 ) ).thenReturn( 7 );

		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final SqlStatementLogger statementLogger = mock( SqlStatementLogger.class );
		final StatementObserver statementObserver = mock( StatementObserver.class );
		when( factory.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getSqlStatementLogger() ).thenReturn( statementLogger );
		when( factory.getStatementObserver() ).thenReturn( statementObserver );

		final Timeout timeout = ConnectionLockTimeoutOperations.query(
				"select provider_timeout",
				row -> Timeout.seconds( row.getInt( 1 ) ),
				connection,
				factory
		);
		assertEquals( Timeout.seconds( 7 ).milliseconds(), timeout.milliseconds() );
		ConnectionLockTimeoutOperations.execute( 9, "set provider_timeout %s", connection, factory );

		verify( statementLogger ).logStatement( "select provider_timeout" );
		verify( statementObserver ).performingSql( "select provider_timeout", -1 );
		verify( statement ).execute( "set provider_timeout 9" );
		verify( statement, times( 2 ) ).close();
	}

	@Test
	void connectionQueryRejectsAnEmptyResult() throws Exception {
		final Connection connection = mock( Connection.class );
		final Statement statement = mock( Statement.class );
		final ResultSet resultSet = mock( ResultSet.class );
		when( connection.createStatement() ).thenReturn( statement );
		when( statement.executeQuery( "select provider_timeout" ) ).thenReturn( resultSet );
		when( resultSet.next() ).thenReturn( false );

		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( factory.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getSqlStatementLogger() ).thenReturn( mock( SqlStatementLogger.class ) );
		when( factory.getStatementObserver() ).thenReturn( mock( StatementObserver.class ) );

		assertThrows(
				HibernateException.class,
				() -> ConnectionLockTimeoutOperations.query(
						"select provider_timeout",
						row -> Timeout.milliseconds( row.getInt( 1 ) ),
						connection,
						factory
				)
		);
	}

	@Test
	void requiredArgumentsFailFast() {
		assertThrows( NullPointerException.class, () -> StandardLockingSupports.db2( null ) );
		assertThrows(
				NullPointerException.class,
				() -> StandardLockingSupports.simple(
						null,
						RowLockStrategy.NONE,
						LockTimeoutType.NONE,
						OuterJoinLockingType.UNSUPPORTED,
						StandardConnectionLockTimeoutStrategies.mysql( 100 )
				)
		);
		assertThrows( IllegalArgumentException.class, () -> StandardConnectionLockTimeoutStrategies.mysql( 0 ) );
	}

	private static void assertProfileParity(LockingSupport expected, LockingSupport actual) {
		assertNotNull( actual );
		assertEquals(
				expected.getMetadata().getPessimisticLockStyle(),
				actual.getMetadata().getPessimisticLockStyle()
		);
		assertEquals( expected.getMetadata().getReadRowLockStrategy(), actual.getMetadata().getReadRowLockStrategy() );
		assertEquals( expected.getMetadata().getWriteRowLockStrategy(), actual.getMetadata().getWriteRowLockStrategy() );
		assertEquals( expected.getMetadata().getOuterJoinLockingType(), actual.getMetadata().getOuterJoinLockingType() );
		for ( Timeout timeout : List.of( Timeouts.WAIT_FOREVER, Timeouts.NO_WAIT, Timeouts.SKIP_LOCKED, Timeout.milliseconds( 1250 ) ) ) {
			assertEquals(
					expected.getMetadata().getLockTimeoutType( timeout ),
					actual.getMetadata().getLockTimeoutType( timeout )
			);
		}
		assertEquals(
				expected.getLockingClauseRenderer().render( UPDATE ),
				actual.getLockingClauseRenderer().render( UPDATE )
		);
		assertEquals(
				expected.getConnectionLockTimeoutStrategy().getSupportedLevel(),
				actual.getConnectionLockTimeoutStrategy().getSupportedLevel()
		);
	}

	private static LockingClauseRequest skipLockedRequest() {
		return new LockingClauseRequest( PessimisticLockKind.UPDATE, Timeouts.SKIP_LOCKED, List.of() );
	}

	private static org.hibernate.dialect.lock.spi.TableLockHintRequest tableHintRequest() {
		return new org.hibernate.dialect.lock.spi.TableLockHintRequest() {
			@Override
			public PessimisticLockKind lockKind() {
				return PessimisticLockKind.UPDATE;
			}

			@Override
			public Timeout timeout() {
				return Timeouts.NO_WAIT;
			}

			@Override
			public String tableExpression() {
				return "provider_table";
			}
		};
	}
}
