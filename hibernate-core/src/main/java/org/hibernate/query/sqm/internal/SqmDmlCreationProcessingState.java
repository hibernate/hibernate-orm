/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * QuerySpecProcessingState implementation for DML statements
 *
 * @author Steve Ebersole
 */
public class SqmDmlCreationProcessingState extends SqmCreationProcessingStateImpl {

	public SqmDmlCreationProcessingState(
			SqmQuery<?> processingQuery,
			SqmCreationState creationState) {
		super( processingQuery, creationState );
	}
}
