/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.internal.exec;

import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;

/// @author Steve Ebersole
public class StandardPlanStepExecutor extends AbstractStepExecutor {
	public StandardPlanStepExecutor(SharedSessionContractImplementor session) {
		super( session );
	}

	@Override
	public void executePreparable(PreparableMutationOperation preparable, FlushOperation flushOperation) {
		executePreparableDirectly( preparable, flushOperation );
	}

}
