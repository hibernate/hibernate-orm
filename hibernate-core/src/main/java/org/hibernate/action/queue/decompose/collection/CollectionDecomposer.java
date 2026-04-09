/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.collection;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

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
	List<PlannedOperation> decomposeRecreate(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext);

	List<PlannedOperation> decomposeUpdate(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext);

	List<PlannedOperation> decomposeRemove(
			CollectionRemoveAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext);
}
