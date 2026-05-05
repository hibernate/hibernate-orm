/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose;

import org.hibernate.TransientPropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.OrphanRemovalAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.exec.DelayedValueAccess;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.action.queue.support.GraphBasedActionQueueFactory;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.hibernate.action.internal.ActionLogging.ACTION_LOGGER;

/// Manages decomposition of [actions][Executable] into table operations for planning.
///
/// <p>Also handles detection and resolution of unresolved transient entity dependencies.
/// When an entity insert references an unsaved transient entity through a non-nullable FK,
/// decomposition is deferred until the dependency is satisfied.
///
/// @implNote Ordering and scheduling are external concerns handled later.
///
/// @author Steve Ebersole
public class Decomposer implements DecompositionContext {
	private final SessionImplementor session;

	// Tracks inserts that have unresolved dependencies on transient entities
	private final Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts = new IdentityHashMap<>();
	// Reverse mapping: transient entity -> inserts waiting for it
	private final Map<Object, Set<AbstractEntityInsertAction>> insertsByTransientEntity = new IdentityHashMap<>();

	// Tracks entities being inserted in the current flush
	// Used to recognize that entities with INSERT actions in this flush are not unresolved dependencies
	private Set<Object> entitiesBeingInserted = null;

	// Tracks entities being deleted in the current flush
	// Used to skip unnecessary UPDATE operations for entities about to be deleted
	private Set<Object> entitiesBeingDeleted = null;
	private Map<Object, int[]> updatedAttributesByDeletedEntity = null;
	private Map<Object, DelayedValueAccess> generatedIdentifierHandles = null;
	private Set<Object> ownersWithUpdateCallbacks = null;

	public Decomposer(SessionImplementor session) {
		this.session = session;
	}

	/// Begin a flush operation - track all entities being inserted/deleted in this flush
	public void beginFlush(
			List<AbstractEntityInsertAction> insertions,
			List<EntityUpdateAction> updates,
			List<OrphanRemovalAction> orphanRemovals,
			List<EntityDeleteAction> deletions) {
		// if this ever shows up as a hot spot (unlikely), we could move collecting these
		// into the action queue proper as the actions are added and then pass into the
		// Decomposer as arguments.
		entitiesBeingInserted = Collections.newSetFromMap(new IdentityHashMap<>());
		entitiesBeingDeleted = Collections.newSetFromMap(new IdentityHashMap<>());
		updatedAttributesByDeletedEntity = new IdentityHashMap<>();
		generatedIdentifierHandles = new IdentityHashMap<>();
		ownersWithUpdateCallbacks = Collections.newSetFromMap(new IdentityHashMap<>());

		for ( AbstractEntityInsertAction insertion : insertions ) {
			final Object instance = insertion.getInstance();
			entitiesBeingInserted.add( instance );
			trackGeneratedIdentifierHandle( insertion );
		}
		for ( EntityDeleteAction deletion : deletions ) {
			entitiesBeingDeleted.add( deletion.getInstance() );
		}
		for ( OrphanRemovalAction orphanRemoval : orphanRemovals ) {
			entitiesBeingDeleted.add( orphanRemoval.getInstance() );
		}
		for ( EntityUpdateAction update : updates ) {
			if ( entitiesBeingDeleted.contains( update.getInstance() ) ) {
				updatedAttributesByDeletedEntity.put( update.getInstance(), update.getDirtyFields() );
			}
		}

		ACTION_LOGGER.tracef("Beginning flush with %d INSERT actions, %d DELETE actions",
			entitiesBeingInserted.size(), entitiesBeingDeleted.size());
	}

	/// End a flush operation - clear the tracking sets
	public void endFlush() {
		entitiesBeingInserted = null;
		entitiesBeingDeleted = null;
		updatedAttributesByDeletedEntity = null;
		ownersWithUpdateCallbacks = null;
	}

	public void clearFlushState() {
		endFlush();
		generatedIdentifierHandles = null;
	}

	@Override
	public boolean isBeingInsertedInCurrentFlush(Object entity) {
		return entitiesBeingInserted != null && entitiesBeingInserted.contains(entity);
	}

	@Override
	public boolean isBeingDeletedInCurrentFlush(Object entity) {
		return entitiesBeingDeleted != null && entitiesBeingDeleted.contains(entity);
	}

	@Override
	public int[] getUpdatedAttributeIndexesForDeletedEntity(Object entity) {
		return updatedAttributesByDeletedEntity == null ? null : updatedAttributesByDeletedEntity.get( entity );
	}

	@Override
	public DelayedValueAccess getGeneratedIdentifierHandle(Object entity) {
		return generatedIdentifierHandles == null ? null : generatedIdentifierHandles.get( entity );
	}

	@Override
	public boolean registerOwnerUpdateCallbacks(Object owner) {
		return ownersWithUpdateCallbacks == null || ownersWithUpdateCallbacks.add( owner );
	}

	/// Filter out dependencies on entities being inserted in the current flush
	/// These are not unresolved - they will be inserted in the same flush
	private NonNullableTransientDependencies filterResolvedDependencies(NonNullableTransientDependencies dependencies) {
		if (dependencies == null || dependencies.isEmpty()) {
			return dependencies;
		}

		if (entitiesBeingInserted == null) {
			// Not in a flush context - return as-is
			return dependencies;
		}

		// Filter out entities being inserted in this flush
		final NonNullableTransientDependencies filtered = new NonNullableTransientDependencies();
		for (Object transientEntity : dependencies.getNonNullableTransientEntities()) {
			if (!isBeingInsertedInCurrentFlush(transientEntity)) {
				// This entity is truly unresolved - not being inserted in this flush
				for (String propertyPath : dependencies.getNonNullableTransientPropertyPaths(transientEntity)) {
					filtered.add(propertyPath, transientEntity);
				}
			}
			else {
				ACTION_LOGGER.tracef("  -> Filtering out dependency on %s (being inserted in this flush)",
					transientEntity.getClass().getSimpleName());
			}
		}

		return filtered;
	}

	public void decompose(
			Executable executable,
			int ordinalBase,
			Consumer<FlushOperation> operationConsumer) {
		// Special handling for entity inserts - check for unresolved transient dependencies
		if (executable instanceof AbstractEntityInsertAction insert) {
			ACTION_LOGGER.tracef("Decomposing INSERT for %s", insert.getEntityName());
			final NonNullableTransientDependencies transientDeps = insert.findNonNullableTransientEntities();

			// Filter out dependencies on entities being inserted in this same flush
			final NonNullableTransientDependencies unresolvedDeps = filterResolvedDependencies(transientDeps);

			if (unresolvedDeps != null && !unresolvedDeps.isEmpty()) {
				// This insert has unresolved dependencies - defer decomposition
				ACTION_LOGGER.tracef("  -> Has unresolved dependencies, deferring");
				trackUnresolvedInsert(insert, unresolvedDeps);
				return;
			}

			ACTION_LOGGER.tracef("  -> No unresolved dependencies, decomposing");
			insert.getPersister().getInsertDecomposer().decompose(
					insert,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}

		if (executable instanceof EntityUpdateAction eua) {
			eua.getPersister().getUpdateDecomposer().decompose(
					eua,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}
		if (executable instanceof EntityDeleteAction eda) {
			eda.getPersister().getDeleteDecomposer().decompose(
					eda,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}

		if (executable instanceof CollectionRecreateAction cra) {
			// MutationKind.INSERT
			cra.getPersister().decompose(
					cra,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}
		if (executable instanceof CollectionRemoveAction cra) {
			// MutationKind.DELETE
			cra.getPersister().decompose(
					cra,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}
		if (executable instanceof CollectionUpdateAction cua) {
			cua.getPersister().decompose(
					cua,
					ordinalBase,
					session,
					this,
					operationConsumer
			);
			return;
		}
		if (executable instanceof QueuedOperationCollectionAction qoca) {
			qoca.getPersister().decompose(
					qoca,
					ordinalBase,
					session,
					operationConsumer
			);
			return;
		}

		throw new UnsupportedOperationException( "Decomposition not supported for " +  executable.getClass().getName() );
	}

	/**
	 * Track an insert action that has unresolved dependencies on transient entities.
	 * This is called for IDENTITY inserts that have transient FK dependencies and need
	 * to be deferred until those dependencies are satisfied.
	 *
	 * @param insert the insert action with unresolved dependencies
	 * @param dependencies the non-nullable transient dependencies
	 */
	public void trackUnresolvedInsert(AbstractEntityInsertAction insert, NonNullableTransientDependencies dependencies) {
		ACTION_LOGGER.tracef("Tracking unresolved insert for %s", insert.getEntityName());
		for (Object transientEntity : dependencies.getNonNullableTransientEntities()) {
			ACTION_LOGGER.tracef("  - depends on: %s@%s",
				transientEntity.getClass().getSimpleName(),
				System.identityHashCode(transientEntity));
		}

		trackGeneratedIdentifierHandle( insert );

		unresolvedInserts.put(insert, dependencies);

		// Build reverse mapping for fast resolution lookup
		for (Object transientEntity : dependencies.getNonNullableTransientEntities()) {
			insertsByTransientEntity
					.computeIfAbsent(transientEntity, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
					.add(insert);
		}
	}

	private void trackGeneratedIdentifierHandle(AbstractEntityInsertAction insert) {
		if ( !insert.getPersister().getGenerator().generatedOnExecution() ) {
			return;
		}

		if ( generatedIdentifierHandles == null ) {
			generatedIdentifierHandles = new IdentityHashMap<>();
		}

		generatedIdentifierHandles.computeIfAbsent(
				insert.getInstance(),
				entity -> new DelayedValueAccess( insert.getEntityName() + "#id" )
		);
	}

	/**
	 * Resolve inserts that were waiting for the specified entity and return the resolved actions.
	 * This should be called after an entity becomes managed to re-add resolved inserts for execution.
	 *
	 * @param managedEntity the entity that just became managed
	 * @return list of AbstractEntityInsertActions that were resolved (all dependencies now satisfied)
	 */
	public List<AbstractEntityInsertAction> resolveDependentActions(Object managedEntity) {
		// Find inserts that were waiting for this entity
		final Set<AbstractEntityInsertAction> dependentInserts = insertsByTransientEntity.remove(managedEntity);

		if (dependentInserts == null || dependentInserts.isEmpty()) {
			return Collections.emptyList();
		}

		final List<AbstractEntityInsertAction> fullyResolved = new ArrayList<>();

		for (AbstractEntityInsertAction entityInsert : dependentInserts) {
			final NonNullableTransientDependencies dependencies = unresolvedInserts.get(entityInsert);

			// Remove this entity from the insert's dependency list
			dependencies.resolveNonNullableTransientEntity(managedEntity);

			// If all dependencies are now satisfied, it can be executed
			if (dependencies.isEmpty()) {
				fullyResolved.add(entityInsert);
			}
		}

		// Clean up fully resolved inserts
		fullyResolved.forEach(unresolvedInserts::remove);

		return fullyResolved;
	}

	/**
	 * Attempt to resolve and decompose inserts that were waiting for the specified entity.
	 * This should be called after an entity becomes managed (e.g., after an INSERT is executed).
	 *
	 * @param managedEntity the entity that just became managed
	 * @param operationConsumer Consumer for any {@linkplain FlushOperation table operations} produced.
	 */
	public void resolveAndDecompose(Object managedEntity, Consumer<FlushOperation> operationConsumer) {
		// Find inserts that were waiting for this entity
		final Set<AbstractEntityInsertAction> dependentInserts = insertsByTransientEntity.remove(managedEntity);

		if (dependentInserts == null || dependentInserts.isEmpty()) {
			return;
		}

		final List<AbstractEntityInsertAction> fullyResolved = new ArrayList<>();

		for (AbstractEntityInsertAction entityInsert : dependentInserts) {
			final NonNullableTransientDependencies dependencies = unresolvedInserts.get(entityInsert);

			// Remove this entity from the insert's dependency list
			dependencies.resolveNonNullableTransientEntity(managedEntity);

			// If all dependencies are now satisfied, decompose it
			if (dependencies.isEmpty()) {
				entityInsert.getPersister().getInsertDecomposer().decompose(
						entityInsert,
						0,
						session,
						this,
						operationConsumer
				);
				fullyResolved.add(entityInsert);
			}
		}

		// Clean up fully resolved inserts
		fullyResolved.forEach(unresolvedInserts::remove);
	}

	/**
	 * Check if there are any unresolved inserts remaining.
	 *
	 * @return true if there are unresolved inserts
	 */
	public boolean hasUnresolvedInserts() {
		return !unresolvedInserts.isEmpty();
	}

	/**
	 * Validate that there are no unresolved inserts remaining.
	 * Throws an exception with details about the first unresolved insert if any remain.
	 *
	 * @throws TransientPropertyValueException if unresolved inserts remain
	 */
	public void validateNoUnresolvedInserts() {
		if (unresolvedInserts.isEmpty()) {
			return;
		}

		// Get first unresolved insert for error reporting
		final var firstEntry = unresolvedInserts.entrySet().iterator().next();
		final AbstractEntityInsertAction firstInsert = firstEntry.getKey();
		final NonNullableTransientDependencies dependencies = firstEntry.getValue();

		// Get first transient dependency for error message
		final Object firstTransient = dependencies.getNonNullableTransientEntities().iterator().next();
		final String firstPropertyPath = dependencies.getNonNullableTransientPropertyPaths(firstTransient).iterator().next();

		final String entityName = firstInsert.getEntityName();
		final String transientEntityName = session.guessEntityName(firstTransient);

		// Log all unresolved dependencies for debugging
		logUnresolvedDependencies();

		throw new TransientPropertyValueException(
				"Instance of '" + entityName +
						"' references an unsaved transient instance of '" + transientEntityName +
						"' (persist the transient instance before flushing)",
				transientEntityName,
				entityName,
				firstPropertyPath
		);
	}

	/**
	 * Log details about all unresolved dependencies for debugging purposes.
	 */
	private void logUnresolvedDependencies() {
		// Log via ActionLogger or similar when available
		// For now, just track the data for error reporting
		// The error message already includes the first unresolved dependency
	}

	/**
	 * Clear all tracked unresolved inserts. Used for cleanup.
	 */
	public void clear() {
		unresolvedInserts.clear();
		insertsByTransientEntity.clear();
		generatedIdentifierHandles = null;
	}

	public void serialize(ObjectOutputStream oos) throws IOException {
		final int queueSize = unresolvedInserts.size();
		ACTION_LOGGER.serializingUnresolvedInsertEntries(queueSize);
		oos.writeInt( queueSize );
		for ( AbstractEntityInsertAction unresolvedAction : unresolvedInserts.keySet() ) {
			oos.writeObject( unresolvedAction );
		}
	}
	public static Decomposer deserialize(
			ObjectInputStream ois,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) throws IOException, ClassNotFoundException {
		var queueSize = ois.readInt();
		ACTION_LOGGER.deserializingUnresolvedInsertEntries(queueSize);

		var actions = CollectionHelper.<AbstractEntityInsertAction>arrayList(queueSize);
		for ( int i = 0; i < queueSize; i++ ) {
			actions.add( (AbstractEntityInsertAction) ois.readObject() );
		}

		return new Decomposer( actions, actionQueueFactory, session );
	}

	/// Deserialization constructor.
	/// @see #deserialize(ObjectInputStream, GraphBasedActionQueueFactory, SessionImplementor)
	private Decomposer(
			ArrayList<AbstractEntityInsertAction> actions,
			GraphBasedActionQueueFactory actionQueueFactory,
			SessionImplementor session) {
		this.session = session;
		actions.forEach( (action) -> trackUnresolvedInsert( action, action.findNonNullableTransientEntities() ) );
	}
}
