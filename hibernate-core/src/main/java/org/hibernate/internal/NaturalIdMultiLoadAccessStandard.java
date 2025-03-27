/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.Collection;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.Status;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Steve Ebersole
 */
public class NaturalIdMultiLoadAccessStandard<T> implements NaturalIdMultiLoadAccess<T>, MultiNaturalIdLoadOptions {
	private final EntityPersister entityDescriptor;
	private final SessionImpl session;

	private LockOptions lockOptions;
	private CacheMode cacheMode;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private Integer batchSize;
	private boolean returnOfDeletedEntitiesEnabled;
	private boolean orderedReturnEnabled = true;

	public NaturalIdMultiLoadAccessStandard(EntityPersister entityDescriptor, SessionImpl session) {
		this.entityDescriptor = entityDescriptor;
		this.session = session;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> withBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
		returnOfDeletedEntitiesEnabled = enabled;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> enableOrderedReturn(boolean enabled) {
		orderedReturnEnabled = enabled;
		return this;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public List<T> multiLoad(Object... ids) {
		performAnyNeededCrossReferenceSynchronizations();

		final CacheMode sessionCacheMode = session.getCacheMode();
		boolean cacheModeChanged = false;

		if ( cacheMode != null ) {
			// naive check for now...
			// todo : account for "conceptually equal"
			if ( cacheMode != sessionCacheMode ) {
				session.setCacheMode( cacheMode );
				cacheModeChanged = true;
			}
		}

		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();

		try {
			final EffectiveEntityGraph effectiveEntityGraph = loadQueryInfluencers.getEffectiveEntityGraph();
			final GraphSemantic initialGraphSemantic = effectiveEntityGraph.getSemantic();
			final RootGraphImplementor<?> initialGraph = effectiveEntityGraph.getGraph();
			final boolean hadInitialGraph = initialGraphSemantic != null;

			if ( graphSemantic != null ) {
				if ( rootGraph == null ) {
					throw new IllegalArgumentException( "Graph semantic specified, but no RootGraph was supplied" );
				}
				effectiveEntityGraph.applyGraph( rootGraph, graphSemantic );
			}

			try {
				return (List<T>) entityDescriptor.getMultiNaturalIdLoader().multiLoad( ids, this, session );
			}
			finally {
				if ( graphSemantic != null ) {
					if ( hadInitialGraph ) {
						effectiveEntityGraph.applyGraph( initialGraph, initialGraphSemantic );
					}
					else {
						effectiveEntityGraph.clear();
					}
				}
			}
		}
		finally {
			if ( cacheModeChanged ) {
				// change it back
				session.setCacheMode( sessionCacheMode );
			}
		}

	}

	protected void performAnyNeededCrossReferenceSynchronizations() {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( !naturalIdMapping.isMutable() ) {
			// only mutable natural-ids need this processing
			return;
		}

		if ( ! session.isTransactionInProgress() ) {
			// not in a transaction so skip synchronization
			return;
		}

		final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
		final Collection<?> cachedPkResolutions =
				persistenceContext.getNaturalIdResolutions()
						.getCachedPkResolutions( entityDescriptor );
		final boolean loggerDebugEnabled = LoaderLogging.LOADER_LOGGER.isDebugEnabled();
		for ( Object pk : cachedPkResolutions ) {
			final EntityKey entityKey = session.generateEntityKey( pk, entityDescriptor );
			final Object entity = persistenceContext.getEntity( entityKey );
			final EntityEntry entry = persistenceContext.getEntry( entity );

			if ( entry == null ) {
				if ( loggerDebugEnabled ) {
					LoaderLogging.LOADER_LOGGER.debugf(
							"Cached natural-id/pk resolution linked to null EntityEntry in persistence context : %s#%s",
							entityDescriptor.getEntityName(),
							pk
					);
				}
				continue;
			}

			if ( !entry.requiresDirtyCheck( entity ) ) {
				continue;
			}

			// MANAGED is the only status we care about here...
			if ( entry.getStatus() != Status.MANAGED ) {
				continue;
			}

			persistenceContext.getNaturalIdResolutions().handleSynchronization( pk, entity, entityDescriptor );
		}
	}

	@Override
	public List<T> multiLoad(List<?> ids) {
		return multiLoad( ids.toArray( new Object[ 0 ] ) );
	}

	@Override
	public boolean isReturnOfDeletedEntitiesEnabled() {
		return returnOfDeletedEntitiesEnabled;
	}

	@Override
	public boolean isOrderReturnEnabled() {
		return orderedReturnEnabled;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize;
	}
}
