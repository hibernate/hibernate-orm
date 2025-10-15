/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.HashSet;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import static org.hibernate.internal.log.LoggingHelper.toLoggableString;

/**
 * Loads entities from the persistence context or creates proxies if not found there,
 * and initializes all proxies in a batch.
 */
public class BatchInitializeEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer<BatchInitializeEntitySelectFetchInitializer.BatchInitializeEntitySelectFetchInitializerData> {

	public static class BatchInitializeEntitySelectFetchInitializerData extends AbstractBatchEntitySelectFetchInitializerData {
		private HashSet<EntityKey> toBatchLoad;

		public BatchInitializeEntitySelectFetchInitializerData(
				BatchInitializeEntitySelectFetchInitializer initializer,
				RowProcessingState rowProcessingState) {
			super( initializer, rowProcessingState );
		}
	}

	public BatchInitializeEntitySelectFetchInitializer(
			InitializerParent<?> parent,
			ToOneAttributeMapping referencedModelPart,
			NavigablePath fetchedNavigable,
			EntityPersister concreteDescriptor,
			DomainResult<?> keyResult,
			boolean affectedByFilter,
			AssemblerCreationState creationState) {
		super( parent, referencedModelPart, fetchedNavigable, concreteDescriptor, keyResult, affectedByFilter, creationState );
	}

	@Override
	protected InitializerData createInitializerData(RowProcessingState rowProcessingState) {
		return new BatchInitializeEntitySelectFetchInitializerData( this, rowProcessingState );
	}

	@Override
	protected void registerResolutionListener(BatchInitializeEntitySelectFetchInitializerData data) {
		// No-op, because we resolve a proxy
	}

	@Override
	protected void registerToBatchFetchQueue(BatchInitializeEntitySelectFetchInitializerData data) {
		super.registerToBatchFetchQueue( data );
		// Force creating a proxy
		final var entityKey = data.entityKey;
		final Object instance =
				data.getRowProcessingState().getSession()
						.internalLoad( entityKey.getEntityName(), entityKey.getIdentifier(), false, false );
		data.setInstance( instance );
		var toBatchLoad = data.toBatchLoad;
		if ( toBatchLoad == null ) {
			toBatchLoad = data.toBatchLoad = new HashSet<>();
		}
		toBatchLoad.add( entityKey );
	}

	@Override
	public void endLoading(BatchInitializeEntitySelectFetchInitializerData data) {
		super.endLoading( data );
		final var keysToBatchLoad = data.toBatchLoad;
		if ( keysToBatchLoad != null ) {
			final var session = data.getRowProcessingState().getSession();
			for ( var entityKey : keysToBatchLoad ) {
				loadInstance( entityKey, toOneMapping, affectedByFilter, session );
			}
			data.toBatchLoad = null;
		}
	}

	@Override
	public String toString() {
		return "BatchInitializeEntitySelectFetchInitializer("
				+ toLoggableString( getNavigablePath() ) + ")";
	}

}
