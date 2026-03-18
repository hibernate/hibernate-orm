/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.TransientPropertyValueException;
import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.op.PlannedOperation;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SessionImplementor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/// Manages decomposition of [actions][Executable] into table operations for planning.
///
/// <p>Also handles detection and resolution of unresolved transient entity dependencies.
/// When an entity insert references an unsaved transient entity through a non-nullable FK,
/// decomposition is deferred until the dependency is satisfied.
///
/// @implNote Ordering and scheduling are external concerns handled later.
///
/// @author Steve Ebersole
public class Decomposer {
	private final SessionImplementor session;

	// Tracks inserts that have unresolved dependencies on transient entities
	private final Map<AbstractEntityInsertAction, NonNullableTransientDependencies> unresolvedInserts = new IdentityHashMap<>();
	// Reverse mapping: transient entity -> inserts waiting for it
	private final Map<Object, Set<AbstractEntityInsertAction>> insertsByTransientEntity = new IdentityHashMap<>();

	public Decomposer(SessionImplementor session) {
		this.session = session;
	}

	public List<PlannedOperation> decompose(
			Executable executable,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry) {
		// Special handling for entity inserts - check for unresolved transient dependencies
		if (executable instanceof AbstractEntityInsertAction insert) {
			final NonNullableTransientDependencies transientDeps = insert.findNonNullableTransientEntities();

			if (transientDeps != null && !transientDeps.isEmpty()) {
				// This insert has unresolved dependencies - defer decomposition
				trackUnresolvedInsert(insert, transientDeps);
				return Collections.emptyList();
			}

			return insert.getPersister().getInsertDecomposer().decompose(
					insert,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}

		if (executable instanceof EntityUpdateAction eua) {
			return eua.getPersister().getUpdateDecomposer().decompose(
					eua,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}
		if (executable instanceof EntityDeleteAction eda) {
			return eda.getPersister().getDeleteDecomposer().decompose(
					eda,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}

		if (executable instanceof CollectionRecreateAction cra) {
			// MutationKind.INSERT
			return cra.getPersister().decompose(
					cra,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}
		if (executable instanceof CollectionRemoveAction cra) {
			// MutationKind.DELETE
			return cra.getPersister().decompose(
					cra,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}
		if (executable instanceof CollectionUpdateAction cua) {
			return cua.getPersister().decompose(
					cua,
					ordinalBase,
					postExecCallbackRegistry,
					session
			);
		}
		if (executable instanceof QueuedOperationCollectionAction qoca) {
			// QueuedOperationCollectionAction is a special meta-action that represents operations
			// on the collection that are "queued" (adding an element to a uninitialized bag, e.g.).
			// It must execute before any other CollectionAction involving the same collection.
			// Since it calls processQueuedOps() which handles the queued operations directly,
			// it doesn't decompose into standard PlannedOperations.  Execute it immediately and
			// return empty list.

			// todo (ActionQueue2) : Consider adding the decomposition.
			//  	Conceptually we could decompose these queued operations into its constituent PlannedOperations.
			//		That's not particularly difficult.
			//		The only consideration is that these still need to be executed before all other
			//		operations for that collection; so not sure its worth the effort?

			qoca.execute();
			return Collections.emptyList();
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
		unresolvedInserts.put(insert, dependencies);

		// Build reverse mapping for fast resolution lookup
		for (Object transientEntity : dependencies.getNonNullableTransientEntities()) {
			insertsByTransientEntity
					.computeIfAbsent(transientEntity, k -> Collections.newSetFromMap(new IdentityHashMap<>()))
					.add(insert);
		}
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
	 * @return list of PlannedOperations for inserts that were resolved
	 */
	public List<PlannedOperation> resolveAndDecompose(Object managedEntity) {
		// Find inserts that were waiting for this entity
		final Set<AbstractEntityInsertAction> dependentInserts = insertsByTransientEntity.remove(managedEntity);

		if (dependentInserts == null || dependentInserts.isEmpty()) {
			return Collections.emptyList();
		}

		final List<PlannedOperation> resolvedOperations = arrayList(dependentInserts.size());
		final List<AbstractEntityInsertAction> fullyResolved = new ArrayList<>();

		for (AbstractEntityInsertAction entityInsert : dependentInserts) {
			final NonNullableTransientDependencies dependencies = unresolvedInserts.get(entityInsert);

			// Remove this entity from the insert's dependency list
			dependencies.resolveNonNullableTransientEntity(managedEntity);

			// If all dependencies are now satisfied, decompose it
			if (dependencies.isEmpty()) {
				resolvedOperations.addAll(
						entityInsert.getPersister().getInsertDecomposer().decompose(
								entityInsert,
								0,
								postCallback -> {}, // No post-execution callbacks for unresolved inserts
								session
						)
				);
				fullyResolved.add(entityInsert);
			}
		}

		// Clean up fully resolved inserts
		fullyResolved.forEach(unresolvedInserts::remove);

		return resolvedOperations;
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
	}
}
