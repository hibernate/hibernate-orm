/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import jakarta.annotation.Nullable;
import org.hibernate.Incubating;

/**
 * Represents a single request for value generation within a batch.
 * <p>
 * Used as an element of the list passed to
 * {@link BeforeExecutionGenerator#generateBatch}.
 *
 * @since 8.0
 *
 * @see BeforeExecutionGenerator#generateBatch
 */
@Incubating
public interface GenerationRequest {
	GenerationRequest EMPTY = of( null, null );

	/**
	 * The entity instance owning the attribute for which
	 * a value is being generated.
	 */
	@Nullable Object entity();

	/**
	 * The current value of the attribute, or {@code null}.
	 */
	@Nullable Object currentValue();

	/**
	 * Creates a generation request for the given entity and current value.
	 *
	 * @param entity The entity
	 * @param currentValue The current value
	 * @return the new generation request
	 */
	static GenerationRequest of(@Nullable Object entity, @Nullable Object currentValue) {
		record SimpleGenerationRequest(
				Object entity,
				Object currentValue
		) implements GenerationRequest {
		}
		return new SimpleGenerationRequest( entity, currentValue );
	}
}
