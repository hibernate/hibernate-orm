/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * Models the state related to parsing a sqm spec.  As a "linked list" to account for
 * subqueries
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class SqmQueryPartCreationProcessingStateStandardImpl extends SqmCreationProcessingStateImpl {

	private final SqmCreationProcessingState parentState;

	public SqmQueryPartCreationProcessingStateStandardImpl(
			SqmCreationProcessingState parentState,
			SqmQuery<?> processingQuery,
			SqmCreationState creationState) {
		super( processingQuery, creationState );
		this.parentState = parentState;
	}

	@Override
	public SqmCreationProcessingState getParentProcessingState() {
		return parentState;
	}

}
