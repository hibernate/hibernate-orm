/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;


import org.hibernate.action.queue.exec.PostExecutionCallback;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.action.spi.Executable;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;
import java.util.function.Consumer;

/// Handles decomposing for a single [action][Executable] type (delegation).
///
/// @see Decomposer
///
/// @author Steve Ebersole
public interface MutationDecomposer<A extends Executable> {
	/// Decompose the [action][Executable] into its constituent table mutations.
	///
	/// @param action The [action][Executable] to decompose
	/// @param ordinalBase Defines a "slot range" which is used to help order scheduling later.
	/// 		Use small offsets from this base for each [mutation][PlannedOperationGroup#ordinal()].
	/// @param postExecCallbackRegistry Ability to register after operation execution callbacks.
	/// @param session The session from which this request originates.
	List<PlannedOperationGroup> decompose(
			A action,
			int ordinalBase,
			Consumer<PostExecutionCallback> postExecCallbackRegistry,
			SharedSessionContractImplementor session);
}
