/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import java.util.List;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;

/**
 * @author Steve Ebersole
 */
public class NaturalIdMultiLoadAccessStandard<T> implements NaturalIdMultiLoadAccess<T>, MultiNaturalIdLoadOptions {
	private final EntityPersister entityDescriptor;
	private final SharedSessionContractImplementor session;

	private LockOptions lockOptions;
	private CacheMode cacheMode;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private Integer batchSize;
	private boolean returnOfDeletedEntitiesEnabled;
	private boolean orderedReturnEnabled = true;

	public NaturalIdMultiLoadAccessStandard(EntityPersister entityDescriptor, SharedSessionContractImplementor session) {
		this.entityDescriptor = entityDescriptor;
		this.session = session;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
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
	public NaturalIdMultiLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
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
		performAnyNeededCrossReferenceSynchronizations( true, entityDescriptor, session );

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
