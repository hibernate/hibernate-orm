/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Timeout;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.TableLockHintRequest;

/// Provider-side test support which exercises only the public table-lock-hint
/// contract.
///
/// @author Steve Ebersole
final class LockingTestSupport {
	private LockingTestSupport() {
	}

	static String renderTableReference(Dialect dialect, LockOptions lockOptions, String tableExpression) {
		final PessimisticLockKind lockKind = lockOptions.getLockMode() == LockMode.WRITE
				? PessimisticLockKind.UPDATE
				: PessimisticLockKind.interpret( lockOptions.getLockMode() );
		final Timeout timeout = switch ( lockOptions.getLockMode() ) {
			case UPGRADE_NOWAIT, PESSIMISTIC_FORCE_INCREMENT -> Timeouts.NO_WAIT;
			case UPGRADE_SKIPLOCKED -> Timeouts.SKIP_LOCKED;
			default -> lockOptions.getTimeout();
		};
		return tableExpression + dialect.getLockingSupport().getTableLockHintRenderer().render(
				new Request( lockKind, timeout, tableExpression )
		);
	}

	private record Request(
			PessimisticLockKind lockKind,
			Timeout timeout,
			String tableExpression) implements TableLockHintRequest {
	}
}
