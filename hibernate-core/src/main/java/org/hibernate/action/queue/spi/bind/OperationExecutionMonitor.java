/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.bind;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionImplementor;

/// Monitors the physical execution of one or more related flush operations.
///
/// A single monitor may be attached to multiple operations. Implementations are
/// responsible for coalescing those notifications into the appropriate logical
/// execution span.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public interface OperationExecutionMonitor {
	/// Invoked immediately before an operation enters physical execution.
	void beforeExecution(SessionImplementor session);

	/// Invoked after the operation completes successfully.
	void afterSuccessfulExecution(SessionImplementor session);

	/// Invoked when physical execution fails.
	void afterFailedExecution(SessionImplementor session);
}
