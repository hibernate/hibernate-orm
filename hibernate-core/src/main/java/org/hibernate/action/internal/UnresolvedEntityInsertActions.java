/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.action.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.IdentitySet;
import org.hibernate.pretty.MessageHelper;

import org.jboss.logging.Logger;

/**
 * Tracks unresolved entity insert actions.
 *
 * An entity insert action is unresolved if the entity
 * to be inserted has at least one non-nullable association with
 * an unsaved transient entity, and the foreign key points to that
 * unsaved transient entity.
 *
 * These references must be resolved before an insert action can be
 * executed.
 *
 * @author Gail Badner
 */
public class UnresolvedEntityInsertActions {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
				CoreMessageLogger.class,
				UnresolvedEntityInsertActions.class.getName()
	);

	private static final int INIT_SIZE = 5;

	private final Map<AbstractEntityInsertAction,NonNullableTransientDependencies> dependenciesByAction =
			new IdentityHashMap<AbstractEntityInsertAction,NonNullableTransientDependencies>( INIT_SIZE );
	private final Map<Object,Set<AbstractEntityInsertAction>> dependentActionsByTransientEntity =
			new IdentityHashMap<Object,Set<AbstractEntityInsertAction>>( INIT_SIZE );

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
	 * Throws {@link org.hibernate.PropertyValueException} if there are any unresolved
	 * entity insert actions that depend on non-nullable associations with
	 * a transient entity. This method should be called on completion of
	 * an operation (after all cascades are completed) that saves an entity.
	 *
	 * @throws org.hibernate.PropertyValueException if there are any unresolved entity
	 * insert actions; {@link org.hibernate.PropertyValueException#getEntityName()}
	 * and {@link org.hibernate.PropertyValueException#getPropertyName()} will
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

	private void logCannotResolveNonNullableTransientDependencies(SessionImplementor session) {
		for ( Map.Entry<Object,Set<AbstractEntityInsertAction>> entry : dependentActionsByTransientEntity.entrySet() ) {
			final Object transientEntity = entry.getKey();
			final String transientEntityName = session.guessEntityName( transientEntity );
			final Serializable transientEntityId = session.getFactory().getEntityPersister( transientEntityName ).getIdentifier( transientEntity, session );
			final String transientEntityString = MessageHelper.infoString( transientEntityName, transientEntityId );
			final Set<String> dependentEntityStrings = new TreeSet<String>();
			final Set<String> nonNullableTransientPropertyPaths = new TreeSet<String>();
			for ( AbstractEntityInsertAction dependentAction : entry.getValue() ) {
				dependentEntityStrings.add( MessageHelper.infoString( dependentAction.getEntityName(), dependentAction.getId() ) );
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

	@SuppressWarnings({ "unchecked" })
	private void addDependenciesByTransientEntity(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		for ( Object transientEntity : dependencies.getNonNullableTransientEntities() ) {
			Set<AbstractEntityInsertAction> dependentActions = dependentActionsByTransientEntity.get( transientEntity );
			if ( dependentActions == null ) {
				dependentActions = new IdentitySet();
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
	@SuppressWarnings({ "unchecked" })
	public Set<AbstractEntityInsertAction> resolveDependentActions(Object managedEntity, SessionImplementor session) {
		final EntityEntry entityEntry = session.getPersistenceContext().getEntry( managedEntity );
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
						MessageHelper.infoString( entityEntry.getEntityName(), entityEntry.getId() )
				);
			}
			// NOTE EARLY EXIT!
			return Collections.emptySet();
		}
		final Set<AbstractEntityInsertAction> resolvedActions = new IdentitySet(  );
		if ( traceEnabled  ) {
			LOG.tracev(
					"Unresolved inserts before resolving [{0}]: [{1}]",
					MessageHelper.infoString( entityEntry.getEntityName(), entityEntry.getId() ),
					toString()
			);
		}
		for ( AbstractEntityInsertAction dependentAction : dependentActions ) {
			if ( traceEnabled ) {
				LOG.tracev(
						"Resolving insert [{0}] dependency on [{1}]",
						MessageHelper.infoString( dependentAction.getEntityName(), dependentAction.getId() ),
						MessageHelper.infoString( entityEntry.getEntityName(), entityEntry.getId() )
				);
			}
			final NonNullableTransientDependencies dependencies = dependenciesByAction.get( dependentAction );
			dependencies.resolveNonNullableTransientEntity( managedEntity );
			if ( dependencies.isEmpty() ) {
				if ( traceEnabled ) {
					LOG.tracev(
							"Resolving insert [{0}] (only depended on [{1}])",
							dependentAction,
							MessageHelper.infoString( entityEntry.getEntityName(), entityEntry.getId() )
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
					MessageHelper.infoString( entityEntry.getEntityName(), entityEntry.getId() ),
					toString()
			);
		}
		return resolvedActions;
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
	 * Deerialize a {@link UnresolvedEntityInsertActions} object.
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
			SessionImplementor session) throws IOException, ClassNotFoundException {

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
