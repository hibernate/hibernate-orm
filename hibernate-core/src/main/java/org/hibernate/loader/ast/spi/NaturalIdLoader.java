/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/// Loader for [org.hibernate.annotations.NaturalId]
///
/// @author Steve Ebersole
public interface NaturalIdLoader<T> extends EntityLoader, MultiKeyLoader {
	interface Options {
		@Nonnull
		LockMode getLockMode();
		@Nonnull
		Timeout getLockTimeout();
		@Nonnull
		PessimisticLockScope getLockScope();
		@Nonnull
		Locking.FollowOn getLockFollowOn();
	}

	/// Perform the load of the entity by its natural-id
	///
	/// @param naturalIdToLoad The natural-id to load.  One of 2 forms accepted:
	///		* Single-value - valid for entities with a simple (single-valued)
	///			natural-id
	///		* Map - valid for any natural-id load.  The map is each value keyed
	///			by the attribute name that the value corresponds to.  Even though
	///			this form is allowed for simple natural-ids, the single value form
	///			should be used as it is more efficient
	/// @param options The options to apply to the load operation
	/// @param session The session into which the entity is being loaded
	///
	/// @deprecated (since 7.3) : use [#load(Object, Options, SharedSessionContractImplementor)] instead.
	@Nullable
	@Deprecated
	T load(@Nullable Object naturalIdToLoad, @Nonnull NaturalIdLoadOptions options, @Nonnull SharedSessionContractImplementor session);

	/// Perform the load of the entity by its natural-id
	///
	/// @param naturalIdToLoad The [normalized][org.hibernate.metamodel.mapping.NaturalIdMapping#normalizeInput]
	/// 	form of the natural-id.
	/// @param options The options to apply to the load operation
	/// @param session The session into which the entity is being loaded
	@Nullable
	T load(@Nullable Object naturalIdToLoad, @Nonnull Options options, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Resolve the id from natural-id value
	 */
	@Nullable
	Object resolveNaturalIdToId(@Nullable Object naturalIdValue, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Resolve the natural-id value(s) from an id
	 */
	@Nullable
	Object resolveIdToNaturalId(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session);
}
