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
@Incubating(since = "8.0")
public interface GenerationRequest {
	GenerationRequest EMPTY = of( null, null );

	/**
	 * The entity instance owning the attribute for which
	 * a value is being generated.
	 */
	@Nullable Object owner();

	/**
	 * The current value of the attribute, or {@code null}.
	 */
	@Nullable Object currentValue();

	/**
	 * Creates a generation request for the given owner and current value.
	 *
	 * @param owner The owner
	 * @param currentValue The current value
	 * @return the new generation request
	 */
	static GenerationRequest of(@Nullable Object owner, @Nullable Object currentValue) {
		record SimpleGenerationRequest(
				Object owner,
				Object currentValue
		) implements GenerationRequest {
		}
		return new SimpleGenerationRequest( owner, currentValue );
	}
}
