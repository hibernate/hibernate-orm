/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import org.hibernate.query.hql.internal.SqmPathRegistryImpl;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * @author Steve Ebersole
 */
public class SqmCreationProcessingStateImpl implements SqmCreationProcessingState {
	private final SqmCreationState creationState;
	private final SqmQuery<?> processingQuery;

	private final SqmPathRegistryImpl processingIndex;

	public SqmCreationProcessingStateImpl(
			SqmQuery<?> processingQuery,
			SqmCreationState creationState) {
		this.processingQuery = processingQuery;
		this.creationState = creationState;
		this.processingIndex = new SqmPathRegistryImpl( this );
	}

	@Override
	public SqmCreationProcessingState getParentProcessingState() {
		return null;
	}

	@Override
	public SqmQuery<?> getProcessingQuery() {
		return processingQuery;
	}

	@Override
	public SqmCreationState getCreationState() {
		return creationState;
	}

	@Override
	public SqmPathRegistry getPathRegistry() {
		return processingIndex;
	}
}
