package org.hibernate.generator.internal;

import java.util.EnumSet;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.Assigned;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;

/**
 * A generator for assigned properties that require invocation to be delayed after batch generation.
 *
 * @since 8.0
 */
public class DelayedAssignedGenerator extends Assigned implements BeforeExecutionGenerator {

	private final String referencedEntityName;

	public DelayedAssignedGenerator(String referencedEntityName) {
		this.referencedEntityName = referencedEntityName;
	}

	@Override
	public boolean generatesSometimes() {
		return true;
	}

	@Override
	public boolean generatesOnInsert() {
		return true;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.INSERT_ONLY;
	}

	@Override
	public boolean generatedBeforeExecution(Object entity, SharedSessionContractImplementor session) {
		return true;
	}

	@Override
	public boolean requiresGeneration(Object entity, Object currentValue, SharedSessionContractImplementor session) {
		if ( currentValue == null ) {
			return true;
		}
		final EntityPersister entityPersister = session.getEntityPersister( null, currentValue );
		final Object currentIdentifier = entityPersister.getIdentifier( currentValue );
		return entityPersister.getGenerator().requiresGeneration( entity, currentIdentifier, session );
	}

	@Override
	public @Nonnull Object generate(
			@Nonnull SharedSessionContractImplementor session,
			@Nullable Object owner,
			@Nullable Object currentValue,
			@Nonnull EventType eventType) {
		return currentValue;
	}

	@Override
	public boolean supportsBatchGeneration() {
		return true;
	}

	@Override
	public List<BeforeExecutionGenerator> resolveGenerationDependencies(RuntimeModelCreationContext context) {
		final var refGen = context.getDomainModel()
				.getEntityDescriptor( referencedEntityName )
				.getGenerator();
		return refGen instanceof BeforeExecutionGenerator beg ? List.of( beg ) : List.of();
	}

	@Override
	public @Nonnull Object[] generateBatch(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull GenerationRequests requests,
			@Nonnull EventType eventType) {
		final Object[] result = new Object[requests.size()];
		for ( int i = 0; i < requests.size(); i++ ) {
			result[i] = requests.get( i ).currentValue();
		}
		return result;
	}
}
