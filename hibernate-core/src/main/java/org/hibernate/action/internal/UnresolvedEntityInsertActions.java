package org.hibernate.action.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;

import jakarta.annotation.Nonnull;

import org.hibernate.PropertyValueException;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.util.collections.IdentitySet;


import static java.util.Collections.emptySet;
import static org.hibernate.action.internal.ActionLogging.ACTION_LOGGER;
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
	public void addUnresolvedEntityInsertAction(
			@Nonnull AbstractEntityInsertAction insert,
			@Nonnull NonNullableTransientDependencies dependencies) {
		if ( dependencies == null || dependencies.isEmpty() ) {
			throw new IllegalArgumentException(
					"Attempt to add an unresolved insert action that has no non-nullable transient entities."
			);
		}
		if ( ACTION_LOGGER.isTraceEnabled() ) {
			ACTION_LOGGER.addingInsertWithNonNullableTransientEntities(
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
	@Nonnull
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
			ACTION_LOGGER.noEntityInsertActionsHaveNonNullableTransientDependencies();
		}
		else {
			final var firstDependentAction = dependenciesByAction.keySet().iterator().next();

			logCannotResolveNonNullableTransientDependencies( firstDependentAction.getSession() );

			throw unresolvedInsertException( firstDependentAction, dependenciesByAction );
		}
	}

	public static TransientPropertyValueException unresolvedInsertException(
			AbstractEntityInsertAction firstInsert,
			Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts) {
		// Keep the first unresolved association in the structured exception fields.
		final Object firstTarget = unresolvedInserts.get( firstInsert ).getNonNullableTransientEntities().iterator().next();
		return new TransientPropertyValueException(
				describeInsertBlocker( firstInsert, unresolvedInserts ),
				firstInsert.getSession().guessEntityName( firstTarget ), firstInsert.getEntityName(),
				propertyPath( unresolvedInserts, firstInsert, firstTarget ) );
	}

	/** Find a cycle reachable from the reported insert, or an association with no queued insert. */
	private static String describeInsertBlocker(
			AbstractEntityInsertAction firstInsert,
			Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts) {
		final var insertsByInstance = new IdentityHashMap<Object, AbstractEntityInsertAction>();
		final var remainingTargets = new IdentityHashMap<AbstractEntityInsertAction, Iterator<Object>>();
		for ( var entry : unresolvedInserts.entrySet() ) {
			final var insert = entry.getKey();
			insertsByInstance.put( insert.getInstance(), insert );
			remainingTargets.put( insert, entry.getValue().getNonNullableTransientEntities().iterator() );
		}

		AbstractEntityInsertAction missingTargetOwner = null;
		Object missingTarget = null;
		final var path = new ArrayList<AbstractEntityInsertAction>();
		final var positions = new IdentityHashMap<AbstractEntityInsertAction, Integer>();
		path.add( firstInsert );
		positions.put( firstInsert, 0 );
		while ( !path.isEmpty() ) {
			final var insert = path.get( path.size() - 1 );
			final var targets = remainingTargets.get( insert );
			if ( !targets.hasNext() ) {
				positions.remove( path.remove( path.size() - 1 ) );
				continue;
			}
			final Object target = targets.next();
			final var targetInsert = insertsByInstance.get( target );
			if ( targetInsert == null ) {
				if ( missingTargetOwner == null ) {
					missingTargetOwner = insert;
					missingTarget = target;
				}
			}
			else if ( positions.containsKey( targetInsert ) ) {
				final var cycle = path.subList( positions.get( targetInsert ), path.size() );
				return "Cannot insert '" + firstInsert.getEntityName() + "' due to a cycle of non-nullable associations: "
						+ describeCycle( cycle, unresolvedInserts );
			}
			else if ( remainingTargets.get( targetInsert ).hasNext() ) {
				positions.put( targetInsert, path.size() );
				path.add( targetInsert );
			}
		}
		assert missingTargetOwner != null;
		final String property = propertyPath( unresolvedInserts, missingTargetOwner, missingTarget );
		final String targetName = firstInsert.getSession().guessEntityName( missingTarget );
		return "Cannot insert '" + firstInsert.getEntityName()
				+ "' because non-null association '" + missingTargetOwner.getEntityName() + "." + property
				+ "' references an unsaved transient instance of '" + targetName
				+ "' (persist the target before flushing)";
	}

	private static String describeCycle(
			List<AbstractEntityInsertAction> cycle,
			Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts) {
		final var path = new StringJoiner( " -> " );
		for ( int i = 0; i < cycle.size(); i++ ) {
			final var insert = cycle.get( i );
			final var next = i + 1 == cycle.size() ? cycle.get( 0 ) : cycle.get( i + 1 );
			path.add( insert.getEntityName() + "." + propertyPath( unresolvedInserts, insert, next.getInstance() ) );
		}
		return path.add( cycle.get( 0 ).getEntityName() ).toString();
	}

	private static String propertyPath(
			Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts,
			AbstractEntityInsertAction insert, Object target) {
		return unresolvedInserts.get( insert ).getNonNullableTransientPropertyPaths( target ).iterator().next();
	}

	private void logCannotResolveNonNullableTransientDependencies(@Nonnull SharedSessionContractImplementor session) {
		for ( var entry : dependentActionsByTransientEntity.entrySet() ) {
			final Object transientEntity = entry.getKey();
			final String transientEntityName = session.guessEntityName( transientEntity );
			final Object transientEntityId =
					session.getFactory().getMappingMetamodel()
							.getEntityDescriptor( transientEntityName )
							.getIdentifier( transientEntity, session );
			final String transientEntityString = infoString( transientEntityName, transientEntityId );
			final Set<String> dependentEntityStrings = new TreeSet<>();
			final Set<String> nonNullableTransientPropertyPaths = new TreeSet<>();
			for ( var dependentAction : entry.getValue() ) {
				dependentEntityStrings.add( infoString( dependentAction.getEntityName(), dependentAction.getId() ) );
				for ( String path :
						dependenciesByAction.get( dependentAction )
								.getNonNullableTransientPropertyPaths( transientEntity ) ) {
					final String fullPath = dependentAction.getEntityName() + '.' + path;
					nonNullableTransientPropertyPaths.add( fullPath );
				}
			}

			ACTION_LOGGER.cannotResolveNonNullableTransientDependencies(
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

	private void addDependenciesByTransientEntity(
			@Nonnull AbstractEntityInsertAction insert,
			@Nonnull NonNullableTransientDependencies dependencies) {
		for ( Object transientEntity : dependencies.getNonNullableTransientEntities() ) {
			var dependentActions = dependentActionsByTransientEntity.get( transientEntity );
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
	public @Nonnull Set<AbstractEntityInsertAction> resolveDependentActions(
			@Nonnull Object managedEntity,
			@Nonnull SessionImplementor session) {
		final var entityEntry = session.getPersistenceContextInternal().getEntry( managedEntity );
		final Status status = entityEntry.getStatus();
		if ( status != Status.MANAGED && status != Status.READ_ONLY ) {
			throw new IllegalArgumentException( "EntityEntry did not have status MANAGED or READ_ONLY: " + entityEntry );
		}

		final boolean traceEnabled = ACTION_LOGGER.isTraceEnabled();
		// Find out if there are any unresolved insertions that are waiting for the
		// specified entity to be resolved.
		final var dependentActions = dependentActionsByTransientEntity.remove( managedEntity );
		if ( dependentActions == null ) {
			if ( traceEnabled ) {
				ACTION_LOGGER.noUnresolvedEntityInsertsThatDependedOn(
						infoString( entityEntry.getEntityName(), entityEntry.getId() )
				);
			}
			return emptySet();
		}
		else {
			final Set<AbstractEntityInsertAction> resolvedActions = new IdentitySet<>();
			if ( traceEnabled  ) {
				ACTION_LOGGER.unresolvedInsertsBeforeResolving(
						infoString( entityEntry.getEntityName(), entityEntry.getId() ),
						toString()
				);
			}
			for ( var dependentAction : dependentActions ) {
				if ( traceEnabled ) {
					ACTION_LOGGER.resolvingInsertDependencyOn(
							infoString( dependentAction.getEntityName(), dependentAction.getId() ),
							infoString( entityEntry.getEntityName(), entityEntry.getId() )
					);
				}
				final var dependencies = dependenciesByAction.get( dependentAction );
				dependencies.resolveNonNullableTransientEntity( managedEntity );
				if ( dependencies.isEmpty() ) {
					if ( traceEnabled ) {
						ACTION_LOGGER.resolvingInsertOnlyDependedOn(
								dependentAction,
								infoString( entityEntry.getEntityName(), entityEntry.getId() )
						);
					}
					// dependentAction only depended on managedEntity
					dependenciesByAction.remove( dependentAction );
					resolvedActions.add( dependentAction );
				}
			}
			if ( traceEnabled  ) {
				ACTION_LOGGER.unresolvedInsertsAfterResolving(
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
	@Nonnull
	public String toString() {
		final var representation = new StringBuilder( getClass().getSimpleName() ).append( '[' );
		for ( var entry : dependenciesByAction.entrySet() ) {
			final AbstractEntityInsertAction insert = entry.getKey();
			final NonNullableTransientDependencies dependencies = entry.getValue();
			representation.append( "[insert=" )
					.append( insert )
					.append( " dependencies=[" )
					.append( dependencies.toLoggableString( insert.getSession() ) )
					.append( "]" );
		}
		representation.append( ']');
		return representation.toString();
	}

	/**
	 * Serialize this {@link UnresolvedEntityInsertActions} object.
	 * @param oos - the output stream
	 * @throws IOException if there is an error writing this object to the output stream.
	 */
	public void serialize(@Nonnull ObjectOutputStream oos) throws IOException {
		final int queueSize = dependenciesByAction.size();
		ACTION_LOGGER.serializingUnresolvedInsertEntries(queueSize);
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
	@Nonnull
	public static UnresolvedEntityInsertActions deserialize(
			@Nonnull ObjectInputStream ois,
			@Nonnull EventSource session)
				throws IOException, ClassNotFoundException {

		final var rtn = new UnresolvedEntityInsertActions();

		final int queueSize = ois.readInt();
		ACTION_LOGGER.deserializingUnresolvedInsertEntries(queueSize);
		for ( int i = 0; i < queueSize; i++ ) {
			final var unresolvedAction = (AbstractEntityInsertAction) ois.readObject();
			unresolvedAction.afterDeserialize( session );
			rtn.addUnresolvedEntityInsertAction(
					unresolvedAction,
					unresolvedAction.findNonNullableTransientEntities()
			);
		}
		return rtn;
	}
}
