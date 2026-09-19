/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;

import jakarta.annotation.Nullable;

/**
 * Coordinates the updating of an entity.
 *
 * @author Steve Ebersole
 * @see #update
 */
public interface UpdateCoordinator extends MutationCoordinator {
	/**
	 * Update a persistent instance.
	 *
	 * @return The {@linkplain GeneratedValues generated values} if any, {@code null} otherwise.
	 */
	@Nullable GeneratedValues update(
			@Nonnull Object entity,
			@Nonnull Object id,
			@Nullable Object rowId,
			@Nonnull Object[] values,
			@Nullable Object oldVersion,
			@Nullable Object[] incomingOldValues,
			@Nullable int[] dirtyAttributeIndexes,
			boolean hasDirtyCollection,
			@Nonnull SharedSessionContractImplementor session);

	void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			@Nonnull SharedSessionContractImplementor session);

	default void forceVersionIncrement(
			@Nonnull Object id,
			@Nullable Object currentVersion,
			@Nonnull Object nextVersion,
			boolean batching,
			@Nonnull SharedSessionContractImplementor session) {
		forceVersionIncrement( id, currentVersion, nextVersion, session );
	}
}
