/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.Objects;

import org.hibernate.SPI;

/// Standard entity-locking strategy factories supplied by Hibernate.
///
/// The returned factories are stable, immutable singletons. A provider should
/// retain and return one of them instead of creating a factory for every call.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(SPI.Role.USE)
public final class EntityLockingStrategies {
	private static final EntityLockingStrategyFactory STANDARD =
			request -> requireRequest( request ).createStrategy( EntityLockingStrategyKind.STANDARD );
	private static final EntityLockingStrategyFactory PESSIMISTIC_SELECT = request -> switch ( requireRequest( request ).lockMode() ) {
		case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED ->
				request.createStrategy( EntityLockingStrategyKind.SELECT );
		default -> STANDARD.createStrategy( request );
	};
	private static final EntityLockingStrategyFactory PESSIMISTIC_UPDATE = request -> switch ( requireRequest( request ).lockMode() ) {
		case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED ->
				request.createStrategy( EntityLockingStrategyKind.UPDATE );
		default -> STANDARD.createStrategy( request );
	};

	private EntityLockingStrategies() {
	}

	/// Hibernate's standard entity-locking behavior.
	public static EntityLockingStrategyFactory standard() {
		return STANDARD;
	}

	/// Uses selecting statements for pessimistic entity locking and standard
	/// behavior for every other lock mode.
	public static EntityLockingStrategyFactory pessimisticSelect() {
		return PESSIMISTIC_SELECT;
	}

	/// Uses updating statements for pessimistic entity locking and standard
	/// behavior for every other lock mode.
	///
	/// Update-based locking requires a versioned entity target.
	public static EntityLockingStrategyFactory pessimisticUpdate() {
		return PESSIMISTIC_UPDATE;
	}

	private static EntityLockingStrategyRequest requireRequest(EntityLockingStrategyRequest request) {
		return Objects.requireNonNull( request, "Entity locking strategy request must not be null" );
	}
}
