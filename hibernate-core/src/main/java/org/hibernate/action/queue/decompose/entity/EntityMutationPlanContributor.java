/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.action.internal.AbstractEntityInsertAction;
import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.ast.TableMutation;

/// Contributes state-management-specific graph mutation plans for entity actions.
///
/// The standard [entity decomposers][EntityActionDecomposer] are responsible for
/// the common action lifecycle: resolving the action state, invoking pre-event
/// handling, coordinating cache handling, assigning operation ordinals, and applying
/// the normal table mutation plan.  A contributor is an extension point for the
/// state-management-specific part of the mutation plan.
///
/// Replacement hooks are used when the logical action is represented by a
/// different complete mutation plan.  For example, soft-delete replaces a
/// logical delete with an update which marks the row as deleted, and
/// single-table temporal update replaces the logical update with an update
/// which ends the current row followed by an insert of the new row version.
/// Returning `true` from a replacement hook means that the contributor has
/// emitted the complete set of [flush operations][FlushOperation] needed for
/// that logical action, including any required pre-/post-execution callbacks
/// supplied through the context.
///
/// Additional hooks are used when the standard mutation plan should still run,
/// but a state-management model needs extra operations.  History-table support
/// is the canonical example: the current table is still inserted, updated, or
/// deleted, and history-table operations are appended to represent that same
/// logical action in the history table.
///
/// Contributors should be kept focused on the mutation plan shape.  They
/// should not duplicate the standard decomposer lifecycle unless the alternate
/// plan itself needs to attach callbacks or cache handling to the operations it
/// emits.
///
/// @see EntityActionDecomposer
/// @see org.hibernate.persister.state.spi.StateManagement
///
/// @author Steve Ebersole
public interface EntityMutationPlanContributor {
	/// The contributor used by the standard state-management model.  It does
	/// not contribute any alternate mutation plans.
	EntityMutationPlanContributor STANDARD = new EntityMutationPlanContributor() {
	};

	/// Optionally replaces the mutation plan for a logical entity update.
	///
	/// @return `true` if the update was fully represented by operations
	/// emitted to `operationConsumer`; `false` to let the standard
	/// update decomposer emit its normal table update operations
	default boolean contributeReplacementUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
		return false;
	}

	/// Optionally replaces the mutation plan for a logical entity delete.
	///
	/// @return `true` if the delete was fully represented by operations
	/// emitted to `operationConsumer`; `false` to let the standard
	/// delete decomposer emit its normal table delete operations
	default boolean contributeReplacementDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
		return false;
	}

	/// Optionally contributes operations to run after the standard entity insert
	/// operations and before insert post-execution handling.
	default void contributeAdditionalInsert(
			InsertContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	/// Optionally contributes operations to run after the standard entity update
	/// operations and before update post-execution handling.
	default void contributeAdditionalUpdate(
			UpdateContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	/// Optionally contributes operations to run after the standard entity delete
	/// operations and before delete post-execution handling.
	default void contributeAdditionalDelete(
			DeleteContext context,
			Consumer<FlushOperation> operationConsumer) {
	}

	/// Returns any static delete operations supplied by this contributor.
	///
	/// Contributors which represent deletes using a static, reusable mutation
	/// plan should expose that plan here so grouping can use the same operation
	/// shape metadata as the physical delete path.
	default Map<String, ? extends TableMutation<?>> getStaticDeleteOperations() {
		return Map.of();
	}

	/// Context passed to [#contributeAdditionalInsert(InsertContext, Consumer)].
	record InsertContext(
			EntityPersister entityPersister,
			AbstractEntityInsertAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Object entity,
			Object identifier,
			Object[] state,
			InsertCacheHandling.CacheInsert cacheInsert) {
	}

	/// Context passed to update contribution hooks.
	record UpdateContext(
			EntityPersister entityPersister,
			EntityUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Object entity,
			Object identifier,
			Object rowId,
			Object[] state,
			Object[] previousState,
			Object previousVersion,
			EntityEntry entityEntry,
			UpdateCacheHandling.CacheUpdate cacheUpdate) {
	}

	/// Context passed to delete contribution hooks.
	record DeleteContext(
			EntityPersister entityPersister,
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Object identifier,
			Object version,
			Object[] state,
			PostDeleteHandling postDeleteHandling) {
	}
}
