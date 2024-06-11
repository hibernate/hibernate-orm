/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.AssemblerCreationState;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.InitializerData;
import org.hibernate.sql.results.graph.InitializerParent;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Loads entities from the persistence context or creates proxies if not found there,
 * and initializes all proxies in a batch.
 */
public class BatchInitializeEntitySelectFetchInitializer extends AbstractBatchEntitySelectFetchInitializer<BatchInitializeEntitySelectFetchInitializer.BatchInitializeEntitySelectFetchInitializerData> {

	public static class BatchInitializeEntitySelectFetchInitializerData extends AbstractBatchEntitySelectFetchInitializerData {
		private final Set<EntityKey> toBatchLoad = new HashSet<>();

		public BatchInitializeEntitySelectFetchInitializerData(RowProcessingState rowProcessingState) {
			super( rowProcessingState );
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
		return new BatchInitializeEntitySelectFetchInitializerData( rowProcessingState );
	}

	@Override
	protected void registerResolutionListener(BatchInitializeEntitySelectFetchInitializerData data) {
		// No-op, because we resolve a proxy
	}

	@Override
	protected void registerToBatchFetchQueue(BatchInitializeEntitySelectFetchInitializerData data) {
		super.registerToBatchFetchQueue( data );
		// Force creating a proxy
		data.setInstance( data.getRowProcessingState().getSession().internalLoad(
				data.entityKey.getEntityName(),
				data.entityKey.getIdentifier(),
				false,
				false
		) );
		data.toBatchLoad.add( data.entityKey );
	}

	@Override
	public void endLoading(BatchInitializeEntitySelectFetchInitializerData data) {
		super.endLoading( data );
		final SharedSessionContractImplementor session = data.getRowProcessingState().getSession();
		for ( EntityKey key : data.toBatchLoad ) {
			loadInstance( key, toOneMapping, affectedByFilter, session );
		}
		data.toBatchLoad.clear();
	}

	@Override
	public String toString() {
		return "BatchInitializeEntitySelectFetchInitializer(" + LoggingHelper.toLoggableString( getNavigablePath() ) + ")";
	}

}
