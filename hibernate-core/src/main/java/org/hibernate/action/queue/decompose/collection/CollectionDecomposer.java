/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.internal.QueuedOperationCollectionAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.function.Consumer;

/// Decomposer for [collection actions][org.hibernate.action.internal.CollectionAction].
/// Comes in 2 general flavors -
///
/// * [OneToManyDecomposer] for `one-to-many` collections
/// * [BasicCollectionDecomposer] for `element-collection` and `many-to-many` collections
///
/// @see CollectionJdbcOperations
///
/// @apiNote This is just a marker interface mainly for some unified Javadoc.
///
/// @author Steve Ebersole
public interface CollectionDecomposer {
	/// Decomposes collection (re)create actions.
	void decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer);

	/// Decomposes collection update actions.
	void decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer);

	/// Decomposes collection removal ("delete all") operations.
	void decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<PlannedOperation> operationConsumer);

	/// Decomposes queued collection operations.
	void decomposeQueuedOperations(
			QueuedOperationCollectionAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			Consumer<PlannedOperation> operationConsumer);
}
