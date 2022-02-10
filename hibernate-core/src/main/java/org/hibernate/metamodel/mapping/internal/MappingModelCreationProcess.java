/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.mapping.MappingModelCreationLogger;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonTransientException;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationProcess {
	private final String EOL = System.lineSeparator();

	/**
	 * Triggers creation of the mapping model
	 */
	public static void process(
			Map<String,EntityPersister> entityPersisterMap,
			SqmFunctionRegistry functionRegistry,
			RuntimeModelCreationContext creationContext) {
		final MappingModelCreationProcess process = new MappingModelCreationProcess(
				entityPersisterMap,
				functionRegistry,
				creationContext
		);
		process.execute();
	}

	private final Map<String,EntityPersister> entityPersisterMap;
	private final SqmFunctionRegistry functionRegistry;

	private final RuntimeModelCreationContext creationContext;

	private String currentlyProcessingRole;

	private List<PostInitCallbackEntry> postInitCallbacks;

	private MappingModelCreationProcess(
			Map<String, EntityPersister> entityPersisterMap,
			SqmFunctionRegistry functionRegistry,
			RuntimeModelCreationContext creationContext) {
		this.entityPersisterMap = entityPersisterMap;
		this.functionRegistry = functionRegistry;
		this.creationContext = creationContext;
	}

	public RuntimeModelCreationContext getCreationContext() {
		return creationContext;
	}

	public EntityPersister getEntityPersister(String name) {
		return entityPersisterMap.get( name );
	}

	public SqmFunctionRegistry getSqmFunctionRegistry() {
		return functionRegistry;
	}

	/**
	 * Instance-level trigger for {@link #process}
	 */
	private void execute() {
		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			if ( entityPersister instanceof InFlightEntityMappingType ) {
				( (InFlightEntityMappingType) entityPersister ).linkWithSuperType( this );
			}
		}

		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			currentlyProcessingRole = entityPersister.getEntityName();

			if ( entityPersister instanceof InFlightEntityMappingType ) {
				( (InFlightEntityMappingType) entityPersister ).prepareMappingModel( this );
			}
		}

		executePostInitCallbacks();
	}

	private void executePostInitCallbacks() {
		MappingModelCreationLogger.LOGGER.debugf( "Starting post-init callbacks" );

		Map<PostInitCallbackEntry, Exception> exceptions = new HashMap<>();
		while ( postInitCallbacks != null && !postInitCallbacks.isEmpty() ) {
			// copy to avoid CCME
			final ArrayList<PostInitCallbackEntry> copy = new ArrayList<>( postInitCallbacks );

			// NOTE : this is *not* the same as the lengths between `copy` and `postInitCallbacks`
			boolean anyCompleted = false;

			//noinspection ForLoopReplaceableByForEach
			for ( int i = 0; i < copy.size(); i++ ) {
				final PostInitCallbackEntry callbackEntry = copy.get( i );
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
						MappingModelCreationLogger.LOGGER.debugf(
								"Mapping-model creation encountered non-transient error : %s",
								e
						);
						throw e;
					}
					exceptions.put( callbackEntry, e );

					final String format = "Mapping-model creation encountered (possibly) transient error : %s";
					if ( MappingModelCreationLogger.TRACE_ENABLED ) {
						MappingModelCreationLogger.LOGGER.tracef( e, format, e );
					}
					else {
						MappingModelCreationLogger.LOGGER.debugf( format, e );
					}
				}
			}

			if ( !anyCompleted ) {
				// none of the remaining callbacks could complete fully, this is an error
				final StringBuilder buff = new StringBuilder(
						"PostInitCallback queue could not be processed..."
				);
				postInitCallbacks.forEach(
						callbackEntry -> buff.append( EOL )
								.append( "        - " ).append( callbackEntry )
				);
				buff.append( EOL );

				final IllegalStateException illegalStateException = new IllegalStateException( buff.toString() );

				for ( Map.Entry<PostInitCallbackEntry, Exception> entry : exceptions.entrySet() ) {
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
			MappingModelCreationLogger.LOGGER.debugf(
					"Starting PostInitCallbackEntry : %s",
					description
			);
			return callback.process();
		}

		@Override
		public String toString() {
			return "PostInitCallbackEntry - " + description;
		}
	}
}
