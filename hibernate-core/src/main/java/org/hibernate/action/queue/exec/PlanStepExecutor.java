/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.op.PlannedOperation;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public interface PlanStepExecutor {
	void execute(
			List<PlannedOperation> plannedOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<PlannedOperation> fixupOperationConsumer);

	void finishUp();
}
