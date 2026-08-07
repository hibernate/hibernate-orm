/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GenerationRequests;

/**
 * A context for the current managed operation ({@code persist}, {@code merge}, etc.).
 *
 * @since 8.0
 */
public interface ManagedOperationContext {

	/// The context for {@link BeforeExecutionGenerator} that {@link BeforeExecutionGenerator#supportsBatchGeneration()}
	@Nonnull BatchGenerationContext getBatchGenerationContext();

	/// Batch generate values for registered generators and entities with
	/// {@link BeforeExecutionGenerator#generateBatch(SharedSessionContractImplementor, GenerationRequests, EventType)}
	void resolveBatchGenerators(@Nonnull SharedSessionContractImplementor session);
}
