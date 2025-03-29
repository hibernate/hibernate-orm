/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.spi;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * State related to SQM creation, like {@link SqmCreationState}, but specific
 * to its "current processing" - which generally means specific to each statement
 * and sub-query
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmCreationProcessingState {
	/**
	 * The parent processing state.  May be null for the top-level processing.
	 *
	 * Intended to be used while processing a sub-query to access the processing
	 * state of the context in which the sub-query occurs.
	 */
	SqmCreationProcessingState getParentProcessingState();

	/**
	 * Access to the query currently being processed.  This should be generally considered
	 * an inflight model - we are still in the process of creating the SQM
	 */
	SqmQuery<?> getProcessingQuery();

	/**
	 * The overall SQM creation state
	 */
	SqmCreationState getCreationState();

	/**
	 * SqmPathRegistry associated with this state.
	 */
	SqmPathRegistry getPathRegistry();
}
