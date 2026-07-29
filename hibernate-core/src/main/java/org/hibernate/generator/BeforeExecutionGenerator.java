/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * A generator that is called to produce a value just before a row is written to the database.
 * The {@link #generate} method may execute arbitrary Java code. It may even, in principle,
 * access the database via JDBC. But however it's produced, the generated value is sent to the
 * database via a parameter of a JDBC prepared statement, just like any other field or property
 * value.
 * <p>
 * Any {@link BeforeExecutionGenerator} with {@linkplain #getEventTypes() generation event types}
 * {@link EventTypeSets#INSERT_ONLY} may be used to produce {@linkplain jakarta.persistence.Id
 * identifiers}. The built-in identifier generators all implement the older extension point
 * {@link org.hibernate.id.IdentifierGenerator}, which is a subtype of this interface, but that
 * is no longer a requirement for custom id generators.
 * <p>
 * A custom id generator may be integrated with the program using either:
 * <ul>
 * <li>the meta-annotation {@link org.hibernate.annotations.IdGeneratorType} or
 * <li>the annotation {@link org.hibernate.annotations.GenericGenerator}.
 * </ul>
 * <p>
 * On the other hand, generators for regular fields and properties may be integrated using
 * {@link org.hibernate.annotations.ValueGenerationType}, as for any {@link Generator}.
 *
 * @author Steve Ebersole
 * @author Gavin King
 *
 * @since 6.2
 */
public interface BeforeExecutionGenerator extends Generator {
	/**
	 * Generate a value.
	 *
	 * @param session      The session from which the request originates.
	 * @param owner        The instance of the object owning the attribute for which we are generating a value.
	 * @param currentValue The current value assigned to the property, or {@code null}
	 * @param eventType    The type of event that has triggered generation of a new value
	 * @return The generated value
	 */
	@Nonnull Object generate(
			@Nonnull SharedSessionContractImplementor session,
			@Nullable Object owner,
			@Nullable Object currentValue,
			@Nonnull EventType eventType);

	/**
	 * Whether this generator supports batch generation via {@link #generateBatch}.
	 * <p>
	 * When this returns {@code true}, the runtime may collect multiple entities
	 * and invoke {@link #generateBatch} once instead of calling {@link #generate}
	 * repeatedly. Generators that benefit from batch operations (e.g., fetching
	 * multiple values in one round-trip, or amortizing expensive initialization)
	 * should override this to return {@code true} and provide an optimized
	 * {@link #generateBatch} implementation.
	 *
	 * @return {@code true} if this generator provides an optimized batch
	 *         implementation; {@code false} (default) otherwise.
	 *
	 * @since 8.0
	 */
	@Incubating
	default boolean supportsBatchGeneration() {
		return false;
	}

	/**
	 * Generate values for multiple entities in a single batch invocation.
	 * <p>
	 * Called when the runtime has collected multiple entities that all need
	 * a value from this same generator instance. The default implementation
	 * delegates to {@link #generate} for each request.
	 *
	 * @param session   The session from which the request originates.
	 * @param requests  The generation requests, each providing the entity
	 *                  and current value of the property being generated.
	 * @param eventType The type of event that has triggered generation.
	 * @return An array of generated values, in the same order as {@code requests}.
	 *         Must have exactly {@code requests.size()} elements.
	 *
	 * @since 8.0
	 *
	 * @see #supportsBatchGeneration()
	 */
	@Incubating
	default @Nonnull Object[] generateBatch(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull GenerationRequests requests,
			@Nonnull EventType eventType) {
		final Object[] results = new Object[requests.size()];
		for ( int i = 0; i < requests.size(); i++ ) {
			final GenerationRequest request = requests.get( i );
			results[i] = generate( session, request.entity(), request.currentValue(), eventType );
		}
		return results;
	}

	@Override
	default boolean generatedOnExecution() {
		return false;
	}
}
