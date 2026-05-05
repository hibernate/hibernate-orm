/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.action.queue.plan.FlushOperation;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Steve Ebersole
 */
public interface PlanStepExecutor {
	void execute(
			List<FlushOperation> flushOperations,
			Consumer<Object> newlyManagedEntityConsumer,
			Consumer<FlushOperation> fixupOperationConsumer);

	void finishUp();
}
