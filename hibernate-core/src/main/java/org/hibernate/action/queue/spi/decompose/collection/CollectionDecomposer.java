/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.decompose.collection;

import org.hibernate.Incubating;
import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.function.Consumer;

/// Decomposer for [collection actions][org.hibernate.action.internal.CollectionAction].
/// Comes in 2 general flavors -
///
/// * [OneToManyDecomposer] for `one-to-many` collections
/// * [org.hibernate.action.queue.internal.decompose.collection.BasicCollectionDecomposer] for `element-collection` and `many-to-many` collections
///
/// Collection persisters construct their decomposers directly.
///
/// A decomposer owns collection delta discovery, operation ordering and
/// callback/event handling. State-management-specific SQL shape is isolated through
/// [CollectionMutationPlanContributor]: contributors may replace delete/remove
/// mutation plans, provide strategy-specific bind values and contribute alternate
/// operations for logical value changes.  This keeps temporal, history-table and
/// audit behavior from being baked into the delta walking code.
///
/// @see org.hibernate.action.queue.internal.decompose.Decomposer
/// @see CollectionJdbcOperations
/// @see CollectionMutationPlanContributor
///
/// @apiNote This is just a marker interface mainly for some unified Javadoc.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface CollectionDecomposer {
	/// Decomposes collection (re)create actions.
	void decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer);

	/// Decomposes collection update actions.
	void decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer);

	/// Decomposes collection removal ("delete all") operations.
	void decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer);

	/// Decomposes queued collection operations.
	void decomposeQueuedOperations(
			QueuedOperationCollectionAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<FlushOperation> operationConsumer);
}
