/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import jakarta.persistence.Timeout;

import org.hibernate.SPI;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Acquires an entity lock in the underlying database.
///
/// All built-in implementations assume at least
/// [java.sql.Connection#TRANSACTION_READ_COMMITTED] transaction isolation.
/// Providers may implement and supply a strategy from
/// [EntityLockingStrategyFactory#createStrategy], or select a built-in strategy
/// with [EntityLockingStrategyRequest#createStrategy]. Implementations must not
/// retain the session or arguments supplied to [#lock].
///
/// @see EntityLockingStrategyFactory#createStrategy(EntityLockingStrategyRequest)
/// @see EntityLockingStrategyRequest#createStrategy(EntityLockingStrategyKind)
/// @see org.hibernate.cfg.JdbcSettings#ISOLATION
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface LockingStrategy {
	/// Acquires a lock which endures until the current transaction ends.
	///
	/// @throws StaleObjectStateException if the database row cannot be found
	/// @throws LockingStrategyException if the lock attempt fails
	void lock(Object id, Object version, Object object, Timeout timeout, SharedSessionContractImplementor session)
			throws StaleObjectStateException, LockingStrategyException;
}
