/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import java.util.function.Consumer;

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

	default boolean canBatchGeneratedIdentityInserts() {
		return false;
	}

	/**
	 * Enqueue an insert into the open JDBC batch and register a consumer
	 * to receive the generated identifier when the batch is later flushed.
	 * <p>
	 * The consumer is invoked in {@code addBatch} order during the batch's
	 * {@link java.sql.PreparedStatement#executeBatch executeBatch} call.
	 */
	default void insertDeferred(
			Object entity,
			Object[] values,
			Consumer<Object> generatedIdConsumer,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Generated identity inserts are not batchable" );
	}
}
