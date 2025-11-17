/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.spi;

import org.hibernate.Incubating;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.cte.SqmCteStatement;

/**
 * Models the state pertaining to the creation of a single SQM.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationState {
	/**
	 * Access to the context of the creation
	 */
	SqmCreationContext getCreationContext();

	/**
	 * What options should be applied to the creation
	 */
	SqmCreationOptions getCreationOptions();

	/**
	 * Access to the stack of current creation processing state.
	 *
	 * New items are pushed to this stack as we cross certain
	 * boundaries while creating the SQM.  Generally these boundaries
	 * are specific to top-level statements and sub-queries.
	 */
	Stack<SqmCreationProcessingState> getProcessingStateStack();

	default SqmCreationProcessingState getCurrentProcessingState() {
		return getProcessingStateStack().getCurrent();
	}

	SqmCteStatement<?> findCteStatement(String name);
}
