/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose;

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
}
