/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.values.GeneratedValues;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Coordinates the inserting of an entity.
 *
 * @author Marco Belladelli
 * @see #insert(Object, Object[], SharedSessionContractImplementor)
 * @see #insert(Object, Object, Object[], SharedSessionContractImplementor)
 */
public interface InsertCoordinator extends MutationCoordinator {
	/**
	 * Persist an entity instance with a generated identifier.
	 *
	 * @return The {@linkplain GeneratedValues generated values} if any, {@code null} otherwise.
	 */
	@Nullable GeneratedValues insert(Object entity, Object[] values, SharedSessionContractImplementor session);

	/**
	 * Persist an entity instance using the provided identifier.
	 *
	 * @return The {@linkplain GeneratedValues generated values} if any, {@code null} otherwise.
	 */
	@Nullable GeneratedValues insert(
			Object entity,
			Object id,
			Object[] values,
			SharedSessionContractImplementor session);
}
