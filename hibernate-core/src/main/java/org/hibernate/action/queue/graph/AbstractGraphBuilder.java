/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.graph;

import org.hibernate.action.queue.PlanningOptions;
import org.hibernate.action.queue.constraint.ConstraintModel;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractGraphBuilder implements GraphBuilder {
	protected final ConstraintModel constraintModel;
	protected final PlanningOptions planningOptions;
	protected final SharedSessionContractImplementor session;

	public AbstractGraphBuilder(
			ConstraintModel constraintModel,
			PlanningOptions planningOptions,
			SharedSessionContractImplementor session) {
		this.constraintModel = constraintModel;
		this.planningOptions = planningOptions;
		this.session = session;
	}

	// Convenience accessors for planning options
	protected boolean avoidBreakingDeferrable() {
		return planningOptions.avoidBreakingDeferrable();
	}

	protected boolean ignoreDeferrableForOrdering() {
		return planningOptions.ignoreDeferrableForOrdering();
	}

}
