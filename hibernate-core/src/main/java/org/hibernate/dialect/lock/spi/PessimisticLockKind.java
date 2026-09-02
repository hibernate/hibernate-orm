/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.LockMode;
import org.hibernate.SPI;

/// The focused kind of pessimistic lock requested by SQL rendering.
///
/// This classification intentionally collapses the broader execution-level
/// [LockMode] vocabulary into the SQL distinctions relevant to a dialect.
///
/// @since 8.0
/// @author Christian Beikov
/// @author Steve Ebersole
@SPI
public enum PessimisticLockKind {
	NONE,
	SHARE,
	UPDATE;

	/// Interprets an execution-level lock mode as a pessimistic SQL lock kind.
	public static PessimisticLockKind interpret(LockMode lockMode) {
		return switch ( lockMode ) {
			case PESSIMISTIC_READ -> SHARE;
			case PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED -> UPDATE;
			default -> NONE;
		};
	}
}
