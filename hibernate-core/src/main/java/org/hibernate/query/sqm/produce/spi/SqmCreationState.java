/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;

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
}
