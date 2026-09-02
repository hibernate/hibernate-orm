/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.LockOptions;
import org.hibernate.dialect.lock.spi.LockingClauseRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteRequest;
import org.hibernate.dialect.lock.spi.LockingSqlRewriteResult;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.PessimisticLockKind;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.internal.util.collections.CollectionHelper;

/// Internal adaptation between execution-level lock options and the focused
/// completed-SQL locking SPI.
///
/// @since 8.0
/// @author Steve Ebersole
@Internal
public final class LockingSqlRewriterSupport {
	private LockingSqlRewriterSupport() {
	}

	/// Rewrites completed SQL using the supplied locking profile.
	public static LockingSqlRewriteResult rewrite(
			LockingSupport lockingSupport,
			String sql,
			LockOptions lockOptions,
			Map<String, String[]> keyColumnNames) {
		final PessimisticLockKind lockKind = interpretLockKind( lockOptions );
		return lockingSupport.getLockingSqlRewriter().rewrite(
				new LockingSqlRewriteRequest(
						sql,
						lockKind,
						effectiveTimeout( lockOptions ),
						lockingTargets( lockingSupport, lockKind, keyColumnNames )
				)
		);
	}

	/// Interprets the focused lock kind requested by execution-level options.
	public static PessimisticLockKind interpretLockKind(LockOptions lockOptions) {
		return TableLockHintRendererSupport.interpretLockKind( lockOptions.getLockMode() );
	}

	/// Resolves the effective timeout requested by execution-level options.
	public static jakarta.persistence.Timeout effectiveTimeout(LockOptions lockOptions) {
		return TableLockHintRendererSupport.effectiveTimeout( lockOptions );
	}

	private static List<LockingClauseRequest.Target> lockingTargets(
			LockingSupport lockingSupport,
			PessimisticLockKind lockKind,
			Map<String, String[]> keyColumnNames) {
		if ( lockKind == PessimisticLockKind.NONE || CollectionHelper.isEmpty( keyColumnNames ) ) {
			return List.of();
		}
		final RowLockStrategy rowLockStrategy = switch ( lockKind ) {
			case SHARE -> lockingSupport.getMetadata().getReadRowLockStrategy();
			case UPDATE -> lockingSupport.getMetadata().getWriteRowLockStrategy();
			case NONE -> RowLockStrategy.NONE;
		};
		if ( rowLockStrategy == RowLockStrategy.NONE ) {
			return List.of();
		}

		final List<LockingClauseRequest.Target> targets = new ArrayList<>();
		keyColumnNames.forEach( (tableAlias, columns) -> {
			if ( rowLockStrategy == RowLockStrategy.TABLE ) {
				targets.add( new LockingClauseRequest.TableTarget( tableAlias ) );
			}
			else {
				for ( String column : columns ) {
					targets.add( new LockingClauseRequest.ColumnTarget( tableAlias, column ) );
				}
			}
		} );
		return targets;
	}
}
