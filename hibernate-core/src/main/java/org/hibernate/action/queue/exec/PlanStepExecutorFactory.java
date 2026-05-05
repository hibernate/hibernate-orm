/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.exec;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class PlanStepExecutorFactory {
	public static PlanStepExecutor create(SharedSessionContractImplementor session) {
		final Integer configuredJdbcBatchSize = session.getConfiguredJdbcBatchSize();
		if ( configuredJdbcBatchSize != null && configuredJdbcBatchSize > 1 ) {
			return new BatchingPlanStepExecutor( configuredJdbcBatchSize, session );
		}
		else {
			return new StandardPlanStepExecutor( session );
		}
	}
}
