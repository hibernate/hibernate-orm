/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce;

import org.hibernate.query.sqm.produce.spi.SqmCreationState;

/**
 * State related to SQM creation, like {@link SqmCreationState}, but specific
 * to its "current processing" - which generally means specific to each statement
 * and sub-query
 *
 * @author Steve Ebersole
 */
public interface SqmCreationProcessingState {
	/**
	 * The parent processing state.  May be null for the top-level processing.
	 */
	SqmCreationProcessingState getParentProcessingState();

	/**
	 * The overall SQM creation state
	 */
	SqmCreationState getCreationState();

	/**
	 * SqmPathRegistry associated with this state.
	 */
	SqmPathRegistry getPathRegistry();
}
