/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.Generator;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An {@link UnsavedValueStrategy} wrapper that delegates to another strategy but considers
 * generator context for mixed-timing generators.
 * <p>
 * For {@link BeforeExecutionGenerator generators} implementing the mixed-timing pattern
 * (where {@link BeforeExecutionGenerator#generatedOnExecution()} returns {@code true} but
 * {@link BeforeExecutionGenerator#generatedOnExecution(Object, SharedSessionContractImplementor)}
 * can return {@code false} for manually assigned IDs), this strategy correctly identifies
 * entities with manually assigned IDs as unsaved (transient), even when the ID is non-null.
 *
 * @author Steve Ebersole
 * @author Chintu Kumar
 *
 * @since 6.6
 */
public class GeneratorAwareUnsavedStrategy implements UnsavedValueStrategy {

	private final UnsavedValueStrategy delegate;
	private final Generator generator;

	public GeneratorAwareUnsavedStrategy(UnsavedValueStrategy delegate, Generator generator) {
		this.delegate = delegate;
		this.generator = generator;
	}

	@Override
	public @Nullable Boolean isUnsaved(@Nullable Object test) {
		return delegate.isUnsaved( test );
	}

	@Override
	public @Nullable Boolean isUnsaved(
			@Nullable Object test,
			@Nullable Object entity,
			@Nullable SharedSessionContractImplementor session) {
		// First check the delegate strategy
		final Boolean delegateResult = delegate.isUnsaved( test, entity, session );

		// If the delegate conclusively says "not unsaved" (i.e., saved/detached),
		// but we have a mixed-timing generator, check if the ID was manually assigned
		if ( Boolean.FALSE.equals( delegateResult ) && generator instanceof BeforeExecutionGenerator ) {
			final BeforeExecutionGenerator beforeGen = (BeforeExecutionGenerator) generator;

			// Check if this is a mixed-timing generator (class-level check)
			if ( beforeGen.generatedOnExecution() && entity != null && session != null ) {
				// Check instance-level: if generatedOnExecution returns false, ID was manually assigned
				// Manually assigned ID = entity is still transient (unsaved)
				if ( !beforeGen.generatedOnExecution( entity, session ) ) {
					return Boolean.TRUE;  // Override delegate - entity is unsaved
				}
			}
		}

		return delegateResult;
	}

	@Override
	public @Nullable Object getDefaultValue(@Nullable Object currentValue) {
		return delegate.getDefaultValue( currentValue );
	}
}
