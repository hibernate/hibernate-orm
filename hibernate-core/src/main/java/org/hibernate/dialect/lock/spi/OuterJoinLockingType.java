package org.hibernate.dialect.lock.spi;

import org.hibernate.Incubating;

/// Indicates a Dialect's level of support for applying locks
/// to select statements with joins.
///
/// @apiNote Exhaustive switches must account for [#NON_NULL_TARGETS]. This
/// restriction is independent of the targeting mechanism described by [RowLockStrategy].
/// Existing capability values retain their meanings.
///
/// @author Steve Ebersole
@Incubating(since = "7.1", group = "dialect-locking")
public enum OuterJoinLockingType {
	/// Locks applied to joins are not supported, generally resulting in an
	/// error from the database.
	UNSUPPORTED,

	/// The database will not throw an exception, but the locking is ignored.
	/// This is the case with [org.hibernate.dialect.HSQLDialect][org.hibernate.dialect.HSQLDialect]
	/// and [org.hibernate.dialect.H2Dialect][org.hibernate.dialect.H2Dialect].
	///
	/// @implSpec Should generally be treated the same as [#UNSUPPORTED][#UNSUPPORTED]
	/// since rows we expect to be locked not locked.
	IGNORED,

	/// The attempt to lock will succeed, but only root tables will be locked.
	/// In other words, the lock is not extended to joined tables.
	///
	/// @implSpec Should generally be treated the same as [#UNSUPPORTED][#UNSUPPORTED]
	/// since rows we expect to be locked not locked.
	ROOT_ONLY,

	/// Applying locks to joined rows is supported, acquiring locks on all tables.
	FULL,

	/// Applying locks to joined rows is supported.  Which table rows are locked can be controlled
	/// per table reference, generally via one of:
	///
	///   - [org.hibernate.dialect.lock.spi.RowLockStrategy#TABLE][org.hibernate.dialect.lock.spi.RowLockStrategy#TABLE]
	///   - [org.hibernate.dialect.lock.spi.RowLockStrategy#COLUMN][org.hibernate.dialect.lock.spi.RowLockStrategy#COLUMN]
	///   - [org.hibernate.dialect.lock.PessimisticLockStyle#TABLE_HINT][org.hibernate.dialect.lock.PessimisticLockStyle#TABLE_HINT]
	///
	IDENTIFIED,

	/// Direct locking across outer joins is supported when every explicitly
	/// identified lock target is guaranteed not to be null by the join structure.
	/// This describes absent joined rows, independently of column nullability. Which table rows are locked MUST be
	/// controlled per table reference, generally via one of:
	///
	///   - [org.hibernate.dialect.lock.spi.RowLockStrategy#TABLE][org.hibernate.dialect.lock.spi.RowLockStrategy#TABLE]
	///   - [org.hibernate.dialect.lock.spi.RowLockStrategy#COLUMN][org.hibernate.dialect.lock.spi.RowLockStrategy#COLUMN]
	///   - [org.hibernate.dialect.lock.PessimisticLockStyle#TABLE_HINT][org.hibernate.dialect.lock.PessimisticLockStyle#TABLE_HINT]
	///
	/// @since 8.0
	NON_NULL_TARGETS
}
