/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.Objects;

import org.hibernate.Incubating;
import org.hibernate.SPI;
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

import static org.hibernate.SPI.Role.USE;
import static org.hibernate.dialect.lock.PessimisticLockStyle.TABLE_HINT;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.CONNECTION;
import static org.hibernate.dialect.lock.spi.LockTimeoutType.QUERY;

/// Creates standard pessimistic-locking profiles for a custom Dialect.
///
/// Prefer a named database-family factory when extending a maintained Dialect.
/// The named factories preserve Hibernate's version boundaries and coordinated
/// metadata, renderers, timeout handling, and follow-on policy. Use [#simple]
/// or [#parameterized] only when defining a distinct provider profile.
///
/// Returned profiles are immutable or thread-safe and may be retained for the
/// lifetime of the Dialect. Their concrete implementation is intentionally not
/// part of the provider contract.
///
/// @see org.hibernate.dialect.Dialect#getLockingSupport()
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class StandardLockingSupports {
	private StandardLockingSupports() {
	}

	/// A profile for a database which does not support pessimistic locking.
	public static LockingSupport none() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}

	/// The standard statement-clause profile inherited by [org.hibernate.dialect.Dialect].
	public static LockingSupport standard() {
		return LockingSupportSimple.STANDARD_SUPPORT;
	}

	/// The standard statement-clause profile without outer-join locking.
	public static LockingSupport standardWithoutOuterJoinLocking() {
		return LockingSupportSimple.NO_OUTER_JOIN;
	}

	/// Creates a profile using one timeout classification for every timeout value.
	public static LockingSupport simple(
			PessimisticLockStyle pessimisticLockStyle,
			RowLockStrategy rowLockStrategy,
			LockTimeoutType lockTimeoutType,
			OuterJoinLockingType outerJoinLockingType,
			ConnectionLockTimeoutStrategy connectionLockTimeoutStrategy) {
		return new LockingSupportSimple(
				Objects.requireNonNull( pessimisticLockStyle, "pessimisticLockStyle" ),
				Objects.requireNonNull( rowLockStrategy, "rowLockStrategy" ),
				Objects.requireNonNull( lockTimeoutType, "lockTimeoutType" ),
				Objects.requireNonNull( outerJoinLockingType, "outerJoinLockingType" ),
				Objects.requireNonNull( connectionLockTimeoutStrategy, "connectionLockTimeoutStrategy" )
		);
	}

	/// Creates a statement-clause profile with independent timeout classifications.
	public static LockingSupport parameterized(
			PessimisticLockStyle pessimisticLockStyle,
			RowLockStrategy rowLockStrategy,
			LockTimeoutType waitType,
			LockTimeoutType noWaitType,
			LockTimeoutType skipLockedType,
			OuterJoinLockingType outerJoinLockingType) {
		return new LockingSupportParameterized(
				Objects.requireNonNull( pessimisticLockStyle, "pessimisticLockStyle" ),
				Objects.requireNonNull( rowLockStrategy, "rowLockStrategy" ),
				Objects.requireNonNull( waitType, "waitType" ),
				Objects.requireNonNull( noWaitType, "noWaitType" ),
				Objects.requireNonNull( skipLockedType, "skipLockedType" ),
				Objects.requireNonNull( outerJoinLockingType, "outerJoinLockingType" )
		);
	}

	/// The DB2 LUW profile, including skip-locked support beginning with 11.5.
	public static LockingSupport db2(DatabaseVersion version) {
		return DB2LockingSupport.forDB2( requireVersion( version ).isSameOrAfter( 11, 5 ) );
	}

	/// The DB2 for i profile.
	public static LockingSupport db2i() {
		return DB2LockingSupport.forDB2i();
	}

	/// The historical DB2 for i profile inherited from DB2 LUW.
	public static LockingSupport legacyDb2i() {
		return DB2LockingSupport.forLegacyDB2i();
	}

	/// The DB2 for z/OS profile.
	public static LockingSupport db2z() {
		return DB2LockingSupport.forDB2z();
	}

	/// The H2 profile selected for the given database version.
	public static LockingSupport h2(DatabaseVersion version) {
		return requireVersion( version ).isSameOrAfter( 2, 2, 220 )
				? H2LockingSupport.INSTANCE
				: H2LockingSupport.LEGACY_INSTANCE;
	}

	/// The HANA profile selected for the given database version.
	public static LockingSupport hana(DatabaseVersion version) {
		return HANALockingSupport.forDialectVersion( requireVersion( version ) );
	}

	/// The HSQLDB profile selected for the given database version.
	public static LockingSupport hsql(DatabaseVersion version) {
		return requireVersion( version ).isSameOrAfter( 2 )
				? HSQLLockingSupport.LOCKING_SUPPORT
				: HSQLLockingSupport.NO_CLAUSE_LOCKING_SUPPORT;
	}

	/// The MySQL profile selected for the given database version.
	public static LockingSupport mysql(DatabaseVersion version) {
		return new MySQLLockingSupport( requireVersion( version ) );
	}

	/// The MariaDB profile selected for the given database version.
	public static LockingSupport mariaDb(DatabaseVersion version) {
		return new MariaDBLockingSupport( requireVersion( version ) );
	}

	/// The Oracle profile selected for the given database version.
	public static LockingSupport oracle(DatabaseVersion version) {
		return new OracleLockingSupport( requireVersion( version ) );
	}

	/// The PostgreSQL profile selected for the given database version.
	public static LockingSupport postgresql(DatabaseVersion version) {
		final DatabaseVersion requiredVersion = requireVersion( version );
		return new PostgreSQLLockingSupport(
				requiredVersion.isSameOrAfter( 8, 1 ),
				requiredVersion.isSameOrAfter( 9, 5 )
		);
	}

	/// The CockroachDB profile selected for the given database version.
	public static LockingSupport cockroach(DatabaseVersion version) {
		return requireVersion( version ).isSameOrAfter( 20, 1 )
				? CockroachLockingSupport.COCKROACH_LOCKING_SUPPORT
				: CockroachLockingSupport.LEGACY_COCKROACH_LOCKING_SUPPORT;
	}

	/// The TimesTen profile.
	public static LockingSupport timesTen() {
		return TimesTenLockingSupport.TIMES_TEN_LOCKING_SUPPORT;
	}

	/// The SQL Server profile selected for the given database version.
	public static LockingSupport sqlServer(DatabaseVersion version) {
		final DatabaseVersion requiredVersion = requireVersion( version );
		return new TransactSQLLockingSupport(
				TABLE_HINT,
				CONNECTION,
				QUERY,
				QUERY,
				RowLockStrategy.TABLE,
				OuterJoinLockingType.IDENTIFIED,
				TransactSQLLockingSupport.SQLServerImpl.IMPL,
				TransactSQLLockingSupport.sqlServerTableLockHintRenderer( requiredVersion )
		);
	}

	/// The Sybase profile.
	public static LockingSupport sybase() {
		return TransactSQLLockingSupport.SYBASE;
	}

	/// The Sybase ASE profile.
	public static LockingSupport sybaseAse() {
		return TransactSQLLockingSupport.SYBASE_ASE;
	}

	/// The historical Sybase ASE profile.
	public static LockingSupport legacySybaseAse() {
		return TransactSQLLockingSupport.SYBASE_LEGACY;
	}

	/// The Sybase Anywhere profile selected for the given database version.
	public static LockingSupport sybaseAnywhere(DatabaseVersion version) {
		return TransactSQLLockingSupport.forSybaseAnywhere( requireVersion( version ) );
	}

	private static DatabaseVersion requireVersion(DatabaseVersion version) {
		return Objects.requireNonNull( version, "version" );
	}
}
