/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;

import org.jboss.logging.Logger;

import static java.util.Collections.emptySet;
import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * Tracks unresolved entity insert actions.
 * <p>
 * An entity insert action is unresolved if the entity
 * to be inserted has at least one non-nullable association with
 * an unsaved transient entity, and the foreign key points to that
 * unsaved transient entity.
 * <p>
 * These references must be resolved before an insert action can be
 * executed.
 *
 * @author Gail Badner
 */
public class UnresolvedEntityInsertActions {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			UnresolvedEntityInsertActions.class.getName()
	);

	private static final int INIT_SIZE = 5;

	private final Map<AbstractEntityInsertAction,NonNullableTransientDependencies> dependenciesByAction = new IdentityHashMap<>( INIT_SIZE );
	private final Map<Object,Set<AbstractEntityInsertAction>> dependentActionsByTransientEntity = new IdentityHashMap<>( INIT_SIZE );

	/**
	 * Add an unresolved insert action.
	 *
	 * @param insert - unresolved insert action.
	 * @param dependencies - non-nullable transient dependencies
	 *                       (must be non-null and non-empty).
	 *
	 * @throws IllegalArgumentException if {@code dependencies is null or empty}.
	 */
	public void addUnresolvedEntityInsertAction(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		if ( dependencies == null || dependencies.isEmpty() ) {
			throw new IllegalArgumentException(
					"Attempt to add an unresolved insert action that has no non-nullable transient entities."
			);
		}
		if ( LOG.isTraceEnabled() ) {
			LOG.tracev(
					"Adding insert with non-nullable, transient entities; insert=[{0}], dependencies=[{1}]",
					insert,
					dependencies.toLoggableString( insert.getSession() )
			);
		}
		dependenciesByAction.put( insert, dependencies );
		addDependenciesByTransientEntity( insert, dependencies );
	}

	/**
	 * Returns the unresolved insert actions.
	 * @return the unresolved insert actions.
	 */
	public Iterable<AbstractEntityInsertAction> getDependentEntityInsertActions() {
		return dependenciesByAction.keySet();
	}

	/**
	 * Throws {@link PropertyValueException} if there are any unresolved
	 * entity insert actions that depend on non-nullable associations with
	 * a transient entity. This method should be called on completion of
	 * an operation (after all cascades are completed) that saves an entity.
	 *
	 * @throws PropertyValueException if there are any unresolved entity
	 * insert actions; {@link PropertyValueException#getEntityName()}
	 * and {@link PropertyValueException#getPropertyName()} will
	 * return the entity name and property value for the first unresolved
	 * entity insert action.
	 */
	public void checkNoUnresolvedActionsAfterOperation() throws PropertyValueException {
		if ( isEmpty() ) {
			LOG.trace( "No entity insert actions have non-nullable, transient entity dependencies." );
		}
		else {
			final AbstractEntityInsertAction firstDependentAction =
					dependenciesByAction.keySet().iterator().next();

			logCannotResolveNonNullableTransientDependencies( firstDependentAction.getSession() );

			final NonNullableTransientDependencies nonNullableTransientDependencies =
					dependenciesByAction.get( firstDependentAction );
			final Object firstTransientDependency =
					nonNullableTransientDependencies.getNonNullableTransientEntities().iterator().next();
			final String firstPropertyPath =
					nonNullableTransientDependencies.getNonNullableTransientPropertyPaths( firstTransientDependency ).iterator().next();

			throw new TransientPropertyValueException(
					"Not-null property references a transient value - transient instance must be saved before current operation",
					firstDependentAction.getSession().guessEntityName( firstTransientDependency ),
					firstDependentAction.getEntityName(),
					firstPropertyPath
			);
		}
	}

	private void logCannotResolveNonNullableTransientDependencies(SharedSessionContractImplementor session) {
		for ( Map.Entry<Object,Set<AbstractEntityInsertAction>> entry : dependentActionsByTransientEntity.entrySet() ) {
			final Object transientEntity = entry.getKey();
			final String transientEntityName = session.guessEntityName( transientEntity );
			final Object transientEntityId =
					session.getFactory().getMappingMetamodel()
							.getEntityDescriptor( transientEntityName )
							.getIdentifier( transientEntity, session );
			final String transientEntityString = infoString( transientEntityName, transientEntityId );
			final Set<String> dependentEntityStrings = new TreeSet<>();
			final Set<String> nonNullableTransientPropertyPaths = new TreeSet<>();
			for ( AbstractEntityInsertAction dependentAction : entry.getValue() ) {
				dependentEntityStrings.add( infoString( dependentAction.getEntityName(), dependentAction.getId() ) );
				for ( String path : dependenciesByAction.get( dependentAction ).getNonNullableTransientPropertyPaths( transientEntity ) ) {
					final String fullPath = dependentAction.getEntityName() + '.' + path;
					nonNullableTransientPropertyPaths.add( fullPath );
				}
			}

			LOG.cannotResolveNonNullableTransientDependencies(
					transientEntityString,
					dependentEntityStrings,
					nonNullableTransientPropertyPaths
			);
		}
	}

	/**
	 * Returns true if there are no unresolved entity insert actions.
	 * @return true, if there are no unresolved entity insert actions; false, otherwise.
	 */
	public boolean isEmpty() {
		return dependenciesByAction.isEmpty();
	}

	private void addDependenciesByTransientEntity(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		for ( Object transientEntity : dependencies.getNonNullableTransientEntities() ) {
			Set<AbstractEntityInsertAction> dependentActions = dependentActionsByTransientEntity.get( transientEntity );
			if ( dependentActions == null ) {
				dependentActions = new IdentitySet<>();
				dependentActionsByTransientEntity.put( transientEntity, dependentActions );
			}
			dependentActions.add( insert );
		}
	}

	/**
	 * Resolve any dependencies on {@code managedEntity}.
	 *
	 * @param managedEntity - the managed entity name
	 * @param session - the session
	 *
	 * @return the insert actions that depended only on the specified entity.
	 *
	 * @throws IllegalArgumentException if {@code managedEntity} did not have managed or read-only status.
	 */
	public Set<AbstractEntityInsertAction> resolveDependentActions(Object managedEntity, SessionImplementor session) {
		final EntityEntry entityEntry = session.getPersistenceContextInternal().getEntry( managedEntity );
		if ( entityEntry.getStatus() != Status.MANAGED && entityEntry.getStatus() != Status.READ_ONLY ) {
			throw new IllegalArgumentException( "EntityEntry did not have status MANAGED or READ_ONLY: " + entityEntry );
		}

		final boolean traceEnabled = LOG.isTraceEnabled();
		// Find out if there are any unresolved insertions that are waiting for the
		// specified entity to be resolved.
		final Set<AbstractEntityInsertAction> dependentActions = dependentActionsByTransientEntity.remove( managedEntity );
		if ( dependentActions == null ) {
			if ( traceEnabled ) {
				LOG.tracev(
						"No unresolved entity inserts that depended on [{0}]",
						infoString( entityEntry.getEntityName(), entityEntry.getId() )
				);
			}
			return emptySet();
		}
		else {
			final Set<AbstractEntityInsertAction> resolvedActions = new IdentitySet<>(  );
			if ( traceEnabled  ) {
				LOG.tracev(
						"Unresolved inserts before resolving [{0}]: [{1}]",
						infoString( entityEntry.getEntityName(), entityEntry.getId() ),
						toString()
				);
			}
			for ( AbstractEntityInsertAction dependentAction : dependentActions ) {
				if ( traceEnabled ) {
					LOG.tracev(
							"Resolving insert [{0}] dependency on [{1}]",
							infoString( dependentAction.getEntityName(), dependentAction.getId() ),
							infoString( entityEntry.getEntityName(), entityEntry.getId() )
					);
				}
				final NonNullableTransientDependencies dependencies = dependenciesByAction.get( dependentAction );
				dependencies.resolveNonNullableTransientEntity( managedEntity );
				if ( dependencies.isEmpty() ) {
					if ( traceEnabled ) {
						LOG.tracev(
								"Resolving insert [{0}] (only depended on [{1}])",
								dependentAction,
								infoString( entityEntry.getEntityName(), entityEntry.getId() )
						);
					}
					// dependentAction only depended on managedEntity..
					dependenciesByAction.remove( dependentAction );
					resolvedActions.add( dependentAction );
				}
			}
			if ( traceEnabled  ) {
				LOG.tracev(
						"Unresolved inserts after resolving [{0}]: [{1}]",
						infoString( entityEntry.getEntityName(), entityEntry.getId() ),
						toString()
				);
			}
			return resolvedActions;
		}
	}

	/**
	 * Clear this {@link UnresolvedEntityInsertActions}.
	 */
	public void clear() {
		dependenciesByAction.clear();
		dependentActionsByTransientEntity.clear();
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( '[' );
		for ( Map.Entry<AbstractEntityInsertAction,NonNullableTransientDependencies> entry : dependenciesByAction.entrySet() ) {
			final AbstractEntityInsertAction insert = entry.getKey();
			final NonNullableTransientDependencies dependencies = entry.getValue();
			sb.append( "[insert=" )
					.append( insert )
					.append( " dependencies=[" )
					.append( dependencies.toLoggableString( insert.getSession() ) )
					.append( "]" );
		}
		sb.append( ']');
		return sb.toString();
	}

	/**
	 * Serialize this {@link UnresolvedEntityInsertActions} object.
	 * @param oos - the output stream
	 * @throws IOException if there is an error writing this object to the output stream.
	 */
	public void serialize(ObjectOutputStream oos) throws IOException {
		final int queueSize = dependenciesByAction.size();
		LOG.tracev( "Starting serialization of [{0}] unresolved insert entries", queueSize );
		oos.writeInt( queueSize );
		for ( AbstractEntityInsertAction unresolvedAction : dependenciesByAction.keySet() ) {
			oos.writeObject( unresolvedAction );
		}
	}

	/**
	 * Deserialize an {@link UnresolvedEntityInsertActions} object.
	 *
	 * @param ois - the input stream.
	 * @param session - the session.
	 *
	 * @return the deserialized  {@link UnresolvedEntityInsertActions} object
	 * @throws IOException if there is an error writing this object to the output stream.
	 * @throws ClassNotFoundException if there is a class that cannot be loaded.
	 */
	public static UnresolvedEntityInsertActions deserialize(
			ObjectInputStream ois,
			EventSource session) throws IOException, ClassNotFoundException {

		final UnresolvedEntityInsertActions rtn = new UnresolvedEntityInsertActions();

		final int queueSize = ois.readInt();
		LOG.tracev( "Starting deserialization of [{0}] unresolved insert entries", queueSize );
		for ( int i = 0; i < queueSize; i++ ) {
			final AbstractEntityInsertAction unresolvedAction = (AbstractEntityInsertAction) ois.readObject();
			unresolvedAction.afterDeserialize( session );
			rtn.addUnresolvedEntityInsertAction(
					unresolvedAction,
					unresolvedAction.findNonNullableTransientEntities()
			);
		}
		return rtn;
	}
}
