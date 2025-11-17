/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonTransientException;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.domain.internal.EntityPersisterConcurrentMap;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

import static org.hibernate.metamodel.mapping.MappingModelCreationLogging.MAPPING_MODEL_CREATION_MESSAGE_LOGGER;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationProcess {
	private final String EOL = System.lineSeparator();

	/**
	 * Triggers creation of the mapping model
	 */
	public static void process(
			EntityPersisterConcurrentMap entityPersisterMap,
			Map<String, CollectionPersister> collectionPersisterMap,
			RuntimeModelCreationContext creationContext) {
		final var process = new MappingModelCreationProcess(
				entityPersisterMap,
				collectionPersisterMap,
				creationContext
		);
		process.execute();
	}

	private final EntityPersisterConcurrentMap entityPersisterMap;
	private final Map<String, CollectionPersister> collectionPersisterMap;
	private final RuntimeModelCreationContext creationContext;

	private String currentlyProcessingRole;
	private List<PostInitCallbackEntry> postInitCallbacks;

	private MappingModelCreationProcess(
			EntityPersisterConcurrentMap entityPersisterMap,
			Map<String, CollectionPersister> collectionPersisterMap,
			RuntimeModelCreationContext creationContext) {
		this.entityPersisterMap = entityPersisterMap;
		this.collectionPersisterMap = collectionPersisterMap;
		this.creationContext = creationContext;
	}

	public RuntimeModelCreationContext getCreationContext() {
		return creationContext;
	}

	public EntityPersister getEntityPersister(String name) {
		return entityPersisterMap.get( name );
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return creationContext.getFunctionRegistry();
	}

	/**
	 * Instance-level trigger for {@link #process}
	 */
	private void execute() {
		for ( var entityPersister : entityPersisterMap.values() ) {
			if ( entityPersister instanceof InFlightEntityMappingType inFlightEntityMappingType ) {
				inFlightEntityMappingType.linkWithSuperType( this );
			}
		}

		for ( var entityPersister : entityPersisterMap.values() ) {
			currentlyProcessingRole = entityPersister.getEntityName();

			if ( entityPersister instanceof InFlightEntityMappingType inFlightEntityMappingType ) {
				inFlightEntityMappingType.prepareMappingModel( this );
			}
		}

		for ( var collectionPersister : collectionPersisterMap.values() ) {
			if ( collectionPersister instanceof InFlightCollectionMapping inFlightCollectionMapping ) {
				inFlightCollectionMapping.prepareMappingModel( this );
			}
		}

		executePostInitCallbacks();
	}

	private void executePostInitCallbacks() {
//		MAPPING_MODEL_CREATION_MESSAGE_LOGGER.tracef( "Starting post-init callbacks" );

		Map<PostInitCallbackEntry, Exception> exceptions = new HashMap<>();
		while ( postInitCallbacks != null && !postInitCallbacks.isEmpty() ) {
			// cope the callback list to avoid CCME
			final var callbacks = new ArrayList<>( postInitCallbacks );

			// NOTE: this is *not* the same as the lengths between `callbacks` and `postInitCallbacks`
			boolean anyCompleted = false;

			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < callbacks.size(); i++ ) {
				final var callbackEntry = callbacks.get( i );
				try {
					final boolean completed = callbackEntry.process();
					if ( completed ) {
						anyCompleted = true;
						postInitCallbacks.remove( callbackEntry );
						exceptions.remove( callbackEntry );
					}
				}
				catch (Exception e) {
					if ( e instanceof NonTransientException ) {
						MAPPING_MODEL_CREATION_MESSAGE_LOGGER.debugf(
								"Mapping-model creation encountered non-transient error: %s",
								e
						);
						throw e;
					}
					exceptions.put( callbackEntry, e );

					final String format = "Mapping-model creation encountered (possibly) transient error: %s";
					if ( MAPPING_MODEL_CREATION_MESSAGE_LOGGER.isTraceEnabled() ) {
						MAPPING_MODEL_CREATION_MESSAGE_LOGGER.tracef( e, format, e );
					}
					else {
						MAPPING_MODEL_CREATION_MESSAGE_LOGGER.debugf( format, e );
					}
				}
			}

			if ( !anyCompleted ) {
				// none of the remaining callbacks could complete fully, this is an error
				final var buff = new StringBuilder(
						"PostInitCallback queue could not be processed..."
				);
				postInitCallbacks.forEach(
						callbackEntry -> buff.append( EOL )
								.append( "        - " ).append( callbackEntry )
				);
				buff.append( EOL );

				final var illegalStateException = new IllegalStateException( buff.toString() );
				for ( var entry : exceptions.entrySet() ) {
					illegalStateException.addSuppressed( entry.getValue() );
				}
				throw illegalStateException;
			}
		}
	}

	public <T extends ModelPart> T processSubPart(
			String localName,
			SubPartMappingProducer<T> subPartMappingProducer) {
		assert currentlyProcessingRole != null;
		final String initialRole = currentlyProcessingRole;
		currentlyProcessingRole = currentlyProcessingRole + '#' + localName;
		try {
			return subPartMappingProducer.produceSubMapping( currentlyProcessingRole, this );
		}
		finally {
			currentlyProcessingRole = initialRole;
		}
	}

	public void registerInitializationCallback(String description, PostInitCallback callback) {
		if ( postInitCallbacks == null ) {
			postInitCallbacks = new ArrayList<>();
		}
		postInitCallbacks.add( new PostInitCallbackEntry( description, callback ) );
	}

	public void registerForeignKeyPostInitCallbacks(String description, PostInitCallback callback) {
		registerInitializationCallback( description, callback );
	}

	private final Map<NavigableRole,ForeignKeyDescriptor> keyDescriptorMap = new HashMap<>();
	private final Map<NavigableRole,List<Consumer<ForeignKeyDescriptor>>> keyDescriptorWaitingConsumerMap = new HashMap<>();

	public void withForeignKey(ModelPart keyOwner, Consumer<ForeignKeyDescriptor> consumer) {
		withForeignKey( keyOwner.getNavigableRole(), consumer );
	}

	private void withForeignKey(NavigableRole navigableRole, Consumer<ForeignKeyDescriptor> consumer) {
		final var keyDescriptor = keyDescriptorMap.get( navigableRole );
		if ( keyDescriptor != null ) {
			consumer.accept( keyDescriptor );
		}
		else {
			final var existingConsumers = keyDescriptorWaitingConsumerMap.get( navigableRole );
			final List<Consumer<ForeignKeyDescriptor>> consumers;
			if ( existingConsumers != null ) {
				consumers = existingConsumers;
			}
			else {
				consumers = new ArrayList<>();
				keyDescriptorWaitingConsumerMap.put( navigableRole, consumers );
			}
			consumers.add( consumer );
		}
	}

	public void registerForeignKey(ModelPart keyOwner, ForeignKeyDescriptor keyDescriptor) {
		final var navigableRole = keyOwner.getNavigableRole();
		keyDescriptorMap.put( navigableRole, keyDescriptor );
		final var waitingConsumers = keyDescriptorWaitingConsumerMap.remove( navigableRole );
		if ( waitingConsumers != null ) {
			for ( int i = 0; i < waitingConsumers.size(); i++ ) {
				waitingConsumers.get( i ).accept( keyDescriptor );
			}
		}
	}

	@FunctionalInterface
	public interface PostInitCallback {
		boolean process();
	}

	/**
	 * Explicitly defined to better control (for now) the args
	 */
	@FunctionalInterface
	public interface SubPartMappingProducer<T> {
		T produceSubMapping(String role, MappingModelCreationProcess creationProcess);
	}

	private static class PostInitCallbackEntry {
		private final String description;
		private final PostInitCallback callback;

		public PostInitCallbackEntry(String description, PostInitCallback callback) {
			this.description = description;
			this.callback = callback;
		}

		private boolean process() {
//			MAPPING_MODEL_CREATION_MESSAGE_LOGGER.tracef(
//					"Starting PostInitCallbackEntry : %s",
//					description
//			);
			return callback.process();
		}

		@Override
		public String toString() {
			return "PostInitCallbackEntry - " + description;
		}
	}
}
