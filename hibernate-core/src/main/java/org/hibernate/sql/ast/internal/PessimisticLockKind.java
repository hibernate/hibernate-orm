/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.LockMode;

/**
 * Single-point reference to the type of pessimistic lock to
 * be acquired across multiple LockModes.
 *
 * @author Christian Beikov
 * @author Steve Ebersole
 */
public enum PessimisticLockKind {
	NONE,
	SHARE,
	UPDATE;

	public static PessimisticLockKind interpret(LockMode lockMode) {
		return switch ( lockMode ) {
			case PESSIMISTIC_READ -> SHARE;
			case PESSIMISTIC_WRITE, PESSIMISTIC_FORCE_INCREMENT, UPGRADE_NOWAIT, UPGRADE_SKIPLOCKED -> UPDATE;
			default -> NONE;
		};
	}
}
