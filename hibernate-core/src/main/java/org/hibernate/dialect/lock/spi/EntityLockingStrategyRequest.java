/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import jakarta.persistence.PessimisticLockScope;

import org.hibernate.LockMode;
import org.hibernate.SPI;

/// Immutable facts used to select an entity-locking strategy.
///
/// This request is owned by Hibernate. A factory may inspect it during
/// [EntityLockingStrategyFactory#createStrategy], but must not retain it.
/// Use [#createStrategy] to obtain a built-in strategy without depending on
/// Hibernate's internal strategy implementations.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public interface EntityLockingStrategyRequest {
	/// The entity target being locked.
	EntityLockTarget target();

	/// The requested entity lock mode.
	LockMode lockMode();

	/// The requested pessimistic lock scope.
	PessimisticLockScope lockScope();

	/// Creates a built-in strategy of the requested kind for this request.
	///
	/// @see LockingStrategy
	LockingStrategy createStrategy(EntityLockingStrategyKind kind);
}
