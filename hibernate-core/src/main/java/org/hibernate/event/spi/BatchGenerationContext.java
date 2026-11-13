package org.hibernate.event.spi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.action.internal.DelayedPostInsertIdentifier;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GenerationRequest;
import org.hibernate.generator.GenerationRequests;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;
import static org.hibernate.id.IdentifierGeneratorHelper.SHORT_CIRCUIT_INDICATOR;

/**
 * Collects batch-capable {@link BeforeExecutionGenerator} registrations across
 * multiple entities during the in-memory value generation traversal, then
 * resolves them all at once by calling
 * {@link BeforeExecutionGenerator#generateBatch}.
 * <p>
 * Typical lifecycle:
 * <ol>
 *   <li>Create a context before processing a group of entities</li>
 *   <li>During each entity's {@code preInsertInMemoryValueGeneration} /
 *       {@code preUpdateInMemoryValueGeneration}, call {@link #register}
 *       for batch-capable generators instead of calling
 *       {@link BeforeExecutionGenerator#generate} directly</li>
 *   <li>After all entities have been traversed, call {@link #resolve}
 *       to execute batch generation and distribute results</li>
 * </ol>
 *
 * @since 8.0
 */
public class BatchGenerationContext {

	/**
	 * Sentinel value installed in the state array when a generator is
	 * registered for batch generation. Replaced with the real value
	 * during {@link #resolve}.
	 */
	public static final Object PLACEHOLDER = new Object() {
		@Override
		public String toString() {
			return "<batch-generation-placeholder>";
		}
	};

	private final Map<GeneratorKey, List<PendingGeneration>> pending = new HashMap<>();
	private @Nullable Map<Object, List<BackRefPatchTarget>> backRefPatchTargets;

	/**
	 * Register a batch-capable generator for deferred generation.
	 * <p>
	 * The caller must install {@link #PLACEHOLDER} in the state array
	 * and on the entity at the given property index.
	 *
	 * @param generator the batch-capable generator
	 * @param propertyIndex the property index in the state array
	 * @param entity the entity instance
	 * @param state the entity's state array (values will be updated in place during resolve)
	 * @param persister the entity persister (used to set values back on the entity)
	 * @param eventType the event type (INSERT or UPDATE)
	 */
	public void register(
			BeforeExecutionGenerator generator,
			int propertyIndex,
			Object entity,
			Object[] state,
			EntityPersister persister,
			EventType eventType) {
		register( generator, null, propertyIndex, entity, state[propertyIndex], state, persister, eventType );
	}

	private void register(
			BeforeExecutionGenerator generator,
			@Nullable GeneratedValueConsumer generatedValueConsumer,
			int propertyIndex,
			Object entity,
			@Nullable Object currentValue,
			@Nullable Object[] state,
			EntityPersister persister,
			EventType eventType) {
		pending.computeIfAbsent( new GeneratorKey( generator, eventType ), k -> new ArrayList<>() )
				.add( new PendingGeneration( generatedValueConsumer, propertyIndex, entity, currentValue, state, persister ) );
	}

	/**
	 * Whether any generators have been registered.
	 */
	public boolean isEmpty() {
		return pending.isEmpty();
	}

	/**
	 * Execute batch generation for all registered generators and distribute
	 * results back into the state arrays and entity instances.
	 *
	 * @param session the session
	 */
	public void resolve(@Nonnull SharedSessionContractImplementor session) {
		if ( pending.isEmpty() ) {
			return;
		}
		final var metamodel = session.getFactory().getMappingMetamodel();
		final var sortedEntries = new ArrayList<>( pending.entrySet() );
		sortedEntries.sort( Comparator.comparingInt(
				e -> metamodel.getGeneratorOrder( e.getKey().generator() )
		) );
		for ( var entry : sortedEntries ) {
			resolve( entry, session );
		}
		pending.clear();
	}

	private void resolve(Map.Entry<GeneratorKey, List<PendingGeneration>> entry, @Nonnull SharedSessionContractImplementor session) {
		final GeneratorKey key = entry.getKey();
		final List<PendingGeneration> generations = entry.getValue();
		final Object[] results = key.generator.generateBatch( session, GenerationRequests.of( generations ), key.eventType );
		for ( int i = 0; i < generations.size(); i++ ) {
			final PendingGeneration gen = generations.get( i );
			if ( gen.propertyIndex < 0 ) {
				assert gen.generatedValueConsumer != null;
				final Object id = results[i];
				if ( id == null ) {
					throw new IdentifierGenerationException( "Null id generated for entity '" + gen.persister.getEntityName() + "'" );
				}
				else {
					if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
						// TODO: define toString()s for generators
						EVENT_LISTENER_LOGGER.generatedId(
								gen.persister.getIdentifierType().toLoggableString( id, session.getFactory() ),
								key.generator.getClass().getName()
						);
					}
				}
				if ( id != SHORT_CIRCUIT_INDICATOR ) {
					final EntityEntry oldEntry = session.getPersistenceContextInternal().getEntry( gen.owner );
					final Object oldId = oldEntry != null ? oldEntry.getId() : null;
					gen.generatedValueConsumer.consumeGeneratedValue( id, session );
					if ( oldId instanceof DelayedPostInsertIdentifier ) {
						applyBackRefPatches( oldId, id );
					}
				}
			}
			else if ( gen.generatedValueConsumer != null ) {
				gen.generatedValueConsumer.consumeGeneratedValue( results[i], session );
			}
			else {
				gen.state[gen.propertyIndex] = results[i];
				gen.persister.setValue( gen.owner, gen.propertyIndex, results[i] );
			}
		}
	}

	/**
	 * Register a back-reference state array slot that currently holds
	 * a {@link DelayedPostInsertIdentifier} belonging to the collection
	 * owner whose id has not yet been resolved. When that owner's real
	 * id is resolved during {@link #resolve}, the slot is patched in place.
	 *
	 * @param delayedId the {@link DelayedPostInsertIdentifier} value currently in the slot
	 * @param state the state array containing the back-reference value
	 * @param propertyIndex the index within the state array
	 */
	public void registerBackRefPatchTarget(Object delayedId, Object[] state, int propertyIndex) {
		if ( backRefPatchTargets == null ) {
			backRefPatchTargets = new HashMap<>();
		}
		backRefPatchTargets.computeIfAbsent( delayedId, k -> new ArrayList<>() )
				.add( new BackRefPatchTarget( state, propertyIndex ) );
	}

	private void applyBackRefPatches(Object oldId, Object newId) {
		if ( backRefPatchTargets != null ) {
			final var targets = backRefPatchTargets.remove( oldId );
			if ( targets != null ) {
				for ( var target : targets ) {
					target.state[target.propertyIndex] = newId;
				}
			}
		}
	}

	/**
	 * Returns a reserved slot in the batch generation context that must be filled at a later point in time.
	 * This is useful for batch id generation to ensure ids are assigned in the same order as without batch
	 * generation.
	 *
	 * @param generator the batch-capable generator
	 * @param eventType the event type (INSERT or UPDATE)
	 * @return The reserved batch generation slot for this generator
	 */
	public GeneratorSlot reserveGeneratorSlot(BeforeExecutionGenerator generator, EventType eventType) {
		final List<PendingGeneration> pendingGenerations =
				pending.computeIfAbsent( new GeneratorKey( generator, eventType ), k -> new ArrayList<>() );
		pendingGenerations.add( null );
		return new GeneratorSlotImpl( pendingGenerations, pendingGenerations.size() - 1 );
	}

	private record GeneratorSlotImpl(List<PendingGeneration> pendingGenerations, int position) implements GeneratorSlot {
		@Override
		public void setGenerationTarget(GeneratedValueConsumer generatedValueConsumer, Object entity, @Nullable Object currentValue, EntityPersister persister) {
			pendingGenerations.set( position, new PendingGeneration( generatedValueConsumer, -1, entity, currentValue, null, persister ) );
		}
	}

	/**
	 * A handle to register an entity for batch generation.
	 * Similar to {@link #register(BeforeExecutionGenerator, int, Object, Object[], EntityPersister, EventType)},
	 * except that the slot in the generation queue has already been claimed to ensure a specific order.
	 */
	public interface GeneratorSlot {
		/**
		 * Set the given generation target for this reserved slot in the batch generation queue.
		 * <p>
		 * The caller must install {@link #PLACEHOLDER} in the state array
		 * and on the entity at the given property index.
		 *
		 * @param generatedValueConsumer the consumer for the generated value
		 * @param entity the entity instance
		 * @param currentValue The current property value
		 * @param persister the entity persister (used to set values back on the entity)
		 */
		void setGenerationTarget(
				GeneratedValueConsumer generatedValueConsumer,
				Object entity,
				@Nullable Object currentValue,
				EntityPersister persister);
	}

	/**
	 * Callback to consume batch generated value.
	 */
	public interface GeneratedValueConsumer {
		void consumeGeneratedValue(Object generatedValue, SharedSessionContractImplementor session);
	}

	private record GeneratorKey(BeforeExecutionGenerator generator, EventType eventType) {
	}

	private record PendingGeneration(
			@Nullable GeneratedValueConsumer generatedValueConsumer,
			int propertyIndex,
			Object owner,
			Object currentValue,
			Object[] state,
			EntityPersister persister) implements GenerationRequest {
	}

	private record BackRefPatchTarget(Object[] state, int propertyIndex) {
	}
}
