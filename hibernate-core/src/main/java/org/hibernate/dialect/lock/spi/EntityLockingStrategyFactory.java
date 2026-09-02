/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.SPI;
import org.hibernate.dialect.Dialect;

/// Creates the executable locking strategy for an entity-locking request.
///
/// A Dialect supplies one stable, thread-safe factory from
/// [Dialect#getEntityLockingStrategyFactory()]. The factory must not retain the
/// request, its target, or any strategy it creates. Hibernate creates and
/// caches strategies with the owning entity persister when the lock scope is
/// normal, and creates an uncached strategy for extended lock scope.
///
/// Implementations may select one of Hibernate's built-in strategy kinds by
/// calling [EntityLockingStrategyRequest#createStrategy].
///
/// @see Dialect#getEntityLockingStrategyFactory()
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI({ SPI.Role.IMPLEMENT, SPI.Role.SUPPLY })
public interface EntityLockingStrategyFactory {
	/// Creates a non-null strategy for the given immutable request.
	///
	/// @see LockingStrategy
	LockingStrategy createStrategy(EntityLockingStrategyRequest request);
}
