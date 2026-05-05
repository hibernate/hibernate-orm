/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.decompose.entity;


import org.hibernate.action.queue.decompose.Decomposer;
import org.hibernate.action.queue.decompose.DecompositionContext;
import org.hibernate.action.queue.plan.FlushOperation;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.function.Consumer;

/// Handles decomposing for a single [action][Executable] type (delegation).
///
/// Decomposes actions into individual table operations. Grouping and batching
/// of operations happens later in FlushCoordinator.
///
/// Post-execution callbacks are attached directly to Flush operations and execute
/// inline as operations complete, rather than being registered globally.
///
/// @see Decomposer
///
/// @author Steve Ebersole
public interface EntityActionDecomposer<A extends Executable> {
	/// Decompose the [action][Executable] into its constituent [table operations][FlushOperation].
	///
	/// @param action The [action][Executable] to decompose
	/// @param ordinalBase Defines a "slot range" which is used to help order operations later.
	/// 		Use small offsets from this base for each operation.
	/// @param session The session from which this request originates.
	/// @param decompositionContext The decomposition context tracking entities being inserted (might be null)
	/// @param operationConsumer Consumer for any [table operations][FlushOperation] produced
	void decompose(
			A action,
			int ordinalBase,
			SharedSessionContractImplementor session,
			DecompositionContext decompositionContext,
			Consumer<FlushOperation> operationConsumer);
}
