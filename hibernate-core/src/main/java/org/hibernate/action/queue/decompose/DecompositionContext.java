/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose;

import org.hibernate.action.queue.exec.DelayedValueAccess;

/// Context for decomposition operations, providing information about entities being
/// mutated in the current flush.
///
/// This allows decomposition logic (e.g., [org.hibernate.engine.internal.ForeignKeys.Nullifier])
/// to distinguish between:
/// - Truly unresolved transient entities (not being inserted in this flush)
/// - Entities being inserted in this flush (should not be treated as unresolved)
/// - Entities being deleted in this flush (UPDATEs can be skipped)
///
/// Implemented by [Decomposer] and passed to components that need this context.
///
/// @author Steve Ebersole
public interface DecompositionContext {
	/// Check if an entity is being inserted in the current flush.
	///
	/// @param entity the entity to check
	/// @return true if the entity has an INSERT action in this flush, false otherwise
	boolean isBeingInsertedInCurrentFlush(Object entity);

	/// Check if an entity is being deleted in the current flush.
	///
	/// @param entity the entity to check
	/// @return true if the entity has a DELETE action in this flush, false otherwise
	boolean isBeingDeletedInCurrentFlush(Object entity);

	/**
	 * Attribute indexes updated for an entity that is also deleted in the current flush.
	 * These attributes may already have been changed in the database before the DELETE executes.
	 */
	default int[] getUpdatedAttributeIndexesForDeletedEntity(Object entity) {
		return null;
	}

	/**
	 * Get the generated identifier handle for an entity being inserted in this flush.
	 * <p>
	 * Returns {@code null} when the entity identifier is already known, or when the
	 * entity is not part of this decomposition context.
	 */
	default DelayedValueAccess getGeneratedIdentifierHandle(Object entity) {
		return null;
	}

	/**
	 * Register owner update callbacks for this flush.
	 *
	 * @return {@code true} when callbacks should be fired by the caller; {@code false}
	 * when another action already registered them for the same owner.
	 */
	default boolean registerOwnerUpdateCallbacks(Object owner) {
		return true;
	}
}
