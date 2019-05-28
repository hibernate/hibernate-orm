/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.hql.internal.SqmPathRegistryImpl;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * @author Steve Ebersole
 */
public class SqmCreationProcessingStateImpl implements SqmCreationProcessingState {
	private final SqmCreationState creationState;
	private final SqmQuery<?> processingQuery;

	private final SqmPathRegistryImpl processingIndex = new SqmPathRegistryImpl( this );

	public SqmCreationProcessingStateImpl(
			SqmQuery<?> processingQuery,
			SqmCreationState creationState) {
		this.processingQuery = processingQuery;
		this.creationState = creationState;
	}

	@Override
	public SqmCreationProcessingState getParentProcessingState() {
		return null;
	}

	@Override
	public SqmQuery<?> getProcessingQuery() {
		return processingQuery;
	}

	protected SqmPathRegistryImpl getProcessingIndex() {
		return processingIndex;
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
