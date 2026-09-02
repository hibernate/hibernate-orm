/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import java.util.Objects;
import java.util.Set;

import org.hibernate.Incubating;
import org.hibernate.LockOptions;
import org.hibernate.SPI;
import org.hibernate.dialect.lock.internal.NonLockingClauseStrategy;
import org.hibernate.dialect.lock.internal.StandardLockingClauseStrategy;
import org.hibernate.spi.NavigablePath;

import static org.hibernate.SPI.Role.USE;

/// Creates the standard per-translation locking-clause strategies.
///
/// Use [#none()] when the Dialect applies locking outside a statement-level
/// clause. Use [#standard] to compose a provider-owned [LockingClauseRenderer]
/// with Hibernate's standard root and join target collection.
///
/// @see org.hibernate.dialect.Dialect#getLockingClauseStrategy(org.hibernate.sql.ast.spi.query.select.QuerySpec, LockOptions)
/// @see org.hibernate.dialect.Dialect#buildLockingClauseStrategy(PessimisticLockKind, RowLockStrategy, LockOptions, Set)
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
@SPI(USE)
public final class StandardLockingClauseStrategies {
	private StandardLockingClauseStrategies() {
	}

	/// Returns the reusable strategy which collects no targets and renders nothing.
	public static LockingClauseStrategy none() {
		return NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
	}

	/// Creates a strategy for one SQL AST translation.
	///
	/// The lock options are read when this method is invoked. The root-path set is
	/// copied so later caller mutation cannot change the translation. A null
	/// root-path set, as produced by an SQL AST with no selected entity roots,
	/// is treated as empty.
	public static LockingClauseStrategy standard(
			LockingClauseRenderer lockingClauseRenderer,
			PessimisticLockKind lockKind,
			RowLockStrategy rowLockStrategy,
			LockOptions lockOptions,
			Set<NavigablePath> rootsForLocking) {
		Objects.requireNonNull( lockingClauseRenderer, "lockingClauseRenderer" );
		Objects.requireNonNull( lockKind, "lockKind" );
		Objects.requireNonNull( rowLockStrategy, "rowLockStrategy" );
		Objects.requireNonNull( lockOptions, "lockOptions" );
		if ( lockKind == PessimisticLockKind.NONE ) {
			throw new IllegalArgumentException( "lockKind must describe a pessimistic lock" );
		}
		return new StandardLockingClauseStrategy(
				lockingClauseRenderer,
				lockKind,
				rowLockStrategy,
				lockOptions,
				rootsForLocking == null ? Set.of() : Set.copyOf( rootsForLocking )
		);
	}
}
