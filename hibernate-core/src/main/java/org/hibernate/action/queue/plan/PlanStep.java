/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.op.PlannedOperation;

import java.util.List;

/// Execution step within a FlushPlan containing one or more [operations][#operations()].
/// Operations in this step are independent of operations in other steps.
/// Operations within this step are ordered for efficient execution via JDBC batching.
///
/// @author Steve Ebersole
public interface PlanStep {
	/// The operations for this step.
	List<PlannedOperation> operations();
}
