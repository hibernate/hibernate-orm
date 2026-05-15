/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.decompose.entity;

import org.hibernate.action.queue.spi.decompose.entity.EntityActionDecomposer;

import org.hibernate.action.internal.EntityDeleteAction;
import org.hibernate.action.queue.spi.decompose.DecompositionContext;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.ast.TableMutation;

import java.util.Map;
import java.util.function.Consumer;

/// Decomposer contract for entity delete actions.
///
/// Implementations expose the static delete operation shapes used by grouping.
/// A state-management contributor may supply those operation shapes when a
/// logical delete is represented by a non-DELETE mutation, such as soft-delete.
///
/// @see org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor#getStaticDeleteOperations().
///
/// @author Steve Ebersole
public interface DeleteDecomposer extends EntityActionDecomposer<EntityDeleteAction> {
	/// Static set of table mutations used to perform the entity delete.
	/// These may come from [org.hibernate.action.queue.spi.decompose.entity.EntityMutationPlanContributor] for certain
	/// [org.hibernate.persister.state.spi.StateManagement] strategies.
	Map<String, ? extends TableMutation<?>> getStaticDeleteOperations();

	/// Decompose the delete action into one-or-more [operations][FlushOperation].
	@Override
	void decompose(
			EntityDeleteAction action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer);
}
