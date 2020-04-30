/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.metamodel.mapping.MappingModelCreationLogger;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NonTransientException;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class MappingModelCreationProcess {
	/**
	 * Triggers creation of the mapping model
	 */
	public static void process(
			Map<String,EntityPersister> entityPersisterMap,
			RuntimeModelCreationContext creationContext) {
		final MappingModelCreationProcess process = new MappingModelCreationProcess(
				entityPersisterMap,
				creationContext
		);
		process.execute();
	}

	private final Map<String,EntityPersister> entityPersisterMap;

	private final RuntimeModelCreationContext creationContext;

	private String currentlyProcessingRole;

	private List<PostInitCallback> postInitCallbacks;
	private List<PostInitCallback> foreignKeyPostInitCallbacks;

	private MappingModelCreationProcess(
			Map<String,EntityPersister> entityPersisterMap,
			RuntimeModelCreationContext creationContext) {
		this.entityPersisterMap = entityPersisterMap;
		this.creationContext = creationContext;
	}

	public RuntimeModelCreationContext getCreationContext() {
		return creationContext;
	}

	public EntityPersister getEntityPersister(String name) {
		return entityPersisterMap.get( name );
	}

	/**
	 * Instance-level trigger for {@link #process}
	 */
	private void execute() {
		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			entityPersister.linkWithSuperType( this );
		}

		for ( EntityPersister entityPersister : entityPersisterMap.values() ) {
			currentlyProcessingRole = entityPersister.getEntityName();

			entityPersister.prepareMappingModel( this );
		}

		MappingModelCreationLogger.LOGGER.debugf( "Starting generic post-init callbacks" );
		executePostInitCallbacks( postInitCallbacks );

		MappingModelCreationLogger.LOGGER.debugf( "Starting foreign-key post-init callbacks" );
		executePostInitCallbacks( foreignKeyPostInitCallbacks );
	}

	private void executePostInitCallbacks(List<PostInitCallback> postInitCallbacks) {
		while ( postInitCallbacks != null && !postInitCallbacks.isEmpty() ) {

			// copy to avoid CCME
			final ArrayList<PostInitCallback> copy = new ArrayList<>( postInitCallbacks );

			for ( int i = 0; i < copy.size(); i++ ) {
				final PostInitCallback callback = copy.get( i );

				try {
					final boolean completed = callback.process();
					if ( completed ) {
						postInitCallbacks.remove( callback );
					}
				}
				catch (Exception e) {
					if ( e instanceof NonTransientException ) {
						MappingModelCreationLogger.LOGGER.debugf( "Mapping-model creation encountered non-transient error : %s", e );
						throw e;
					}

					MappingModelCreationLogger.LOGGER.debugf( "Mapping-model creation encountered (possibly) transient error : %s", e );
				}
			}

			if ( copy.size() == postInitCallbacks.size() ) {
				// none of the remaining callbacks could complete fully, this is an error
				throw new IllegalStateException( "No post-init callbacks could complete" );
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

	public void registerInitializationCallback(PostInitCallback callback) {
		if ( postInitCallbacks == null ) {
			postInitCallbacks = new ArrayList<>();
		}
		postInitCallbacks.add( callback );
	}

	public void registerForeignKeyPostInitCallbacks(PostInitCallback callback) {
		if ( foreignKeyPostInitCallbacks == null ) {
			foreignKeyPostInitCallbacks = new ArrayList<>();
		}
		foreignKeyPostInitCallbacks.add( callback );
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
}
