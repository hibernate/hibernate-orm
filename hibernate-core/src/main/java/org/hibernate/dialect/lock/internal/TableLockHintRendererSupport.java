/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import jakarta.persistence.Timeout;

import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.TableLockHintRenderer;
import org.hibernate.dialect.lock.spi.TableLockHintRequest;

/// Internal adaptation between execution-level lock options and the focused
/// table-lock-hint rendering SPI.
///
/// @author Steve Ebersole
/// @since 8.0
@Internal
public final class TableLockHintRendererSupport {
	private TableLockHintRendererSupport() {
	}

	/// Renders a complete table-reference fragment from execution-level lock
	/// options.
	public static String renderTableReference(
			LockingSupport lockingSupport,
			LockOptions lockOptions,
			String tableExpression) {
		return tableExpression + renderHint(
				lockingSupport,
				lockOptions.getLockMode(),
				effectiveTimeout( lockOptions ),
				tableExpression
		);
	}

	/// Renders only the table-lock-hint fragment.
	public static String renderHint(
			LockingSupport lockingSupport,
			LockMode lockMode,
			Timeout timeout,
			String tableExpression) {
		if ( lockingSupport == null ) {
			throw new IllegalStateException( "Locking support must not be null" );
		}
		final TableLockHintRenderer renderer = lockingSupport.getTableLockHintRenderer();
		if ( renderer == null ) {
			throw new IllegalStateException( "Table-lock-hint renderer must not be null" );
		}
		final String hint = renderer.render( new Request(
				interpretLockKind( lockMode ),
				timeout,
				tableExpression
		) );
		if ( hint == null ) {
			throw new IllegalStateException( "Table-lock-hint renderer result must not be null" );
		}
		return hint;
	}

	static Timeout effectiveTimeout(LockOptions lockOptions) {
		return switch ( lockOptions.getLockMode() ) {
			case UPGRADE_NOWAIT, PESSIMISTIC_FORCE_INCREMENT -> Timeouts.NO_WAIT;
			case UPGRADE_SKIPLOCKED -> Timeouts.SKIP_LOCKED;
			default -> lockOptions.getTimeout();
		};
	}

	static PessimisticLockKind interpretLockKind(LockMode lockMode) {
		return lockMode == LockMode.WRITE
				? PessimisticLockKind.UPDATE
				: PessimisticLockKind.interpret( lockMode );
	}

	private record Request(
			PessimisticLockKind lockKind,
			Timeout timeout,
			String tableExpression) implements TableLockHintRequest {
	}
}
