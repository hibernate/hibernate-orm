/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

	/**
	 * Register a batch-capable generator for deferred generation.
	 * <p>
	 * The caller must install {@link #PLACEHOLDER} in the state array
	 * and on the entity at the given property index.
	 *
	 * @param generator the batch-capable generator
	 * @param generatedValueConsumer the consumer for the generated value
	 * @param entity the entity instance
	 * @param currentValue The current property value
	 * @param persister the entity persister (used to set values back on the entity)
	 * @param eventType the event type (INSERT or UPDATE)
	 */
	public void register(
			BeforeExecutionGenerator generator,
			GeneratedValueConsumer generatedValueConsumer,
			Object entity,
			@Nullable Object currentValue,
			EntityPersister persister,
			EventType eventType) {
		register( generator, generatedValueConsumer, -1, entity, currentValue, null, persister, eventType );
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
		for ( Map.Entry<GeneratorKey, List<PendingGeneration>> entry : pending.entrySet() ) {
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
						gen.generatedValueConsumer.consumeGeneratedValue( id, session );
					}
				}
				else if ( gen.generatedValueConsumer != null ) {
					gen.generatedValueConsumer.consumeGeneratedValue( results[i], session );
				}
				else {
					gen.state[gen.propertyIndex] = results[i];
					gen.persister.setValue( gen.entity, gen.propertyIndex, results[i] );
				}
			}
		}
		pending.clear();
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
			Object entity,
			Object currentValue,
			Object[] state,
			EntityPersister persister) implements GenerationRequest {
	}
}
