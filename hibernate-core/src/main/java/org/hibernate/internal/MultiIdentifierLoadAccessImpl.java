/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.internal.LoaderHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;

/**
 * @author Steve Ebersole
 */
class MultiIdentifierLoadAccessImpl<T> implements MultiIdentifierLoadAccess<T>, MultiIdLoadOptions {
	private final SessionImpl session;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private Integer batchSize;
	private boolean sessionCheckingEnabled;
	private boolean returnOfDeletedEntitiesEnabled;
	private boolean orderedReturnEnabled = true;

	public MultiIdentifierLoadAccessImpl(SessionImpl session, EntityPersister entityPersister) {
		this.session = session;
		this.entityPersister = entityPersister;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public final MultiIdentifierLoadAccess<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize;
	}

	@Override
	public MultiIdentifierLoadAccess<T> withBatchSize(int batchSize) {
		if ( batchSize < 1 ) {
			this.batchSize = null;
		}
		else {
			this.batchSize = batchSize;
		}
		return this;
	}

	@Override
	public boolean isSessionCheckingEnabled() {
		return sessionCheckingEnabled;
	}

	@Override
	public boolean isSecondLevelCacheCheckingEnabled() {
		return cacheMode == CacheMode.NORMAL || cacheMode == CacheMode.GET;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled) {
		this.sessionCheckingEnabled = enabled;
		return this;
	}

	@Override
	public boolean isReturnOfDeletedEntitiesEnabled() {
		return returnOfDeletedEntitiesEnabled;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
		this.returnOfDeletedEntitiesEnabled = enabled;
		return this;
	}

	@Override
	public boolean isOrderReturnEnabled() {
		return orderedReturnEnabled;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled) {
		this.orderedReturnEnabled = enabled;
		return this;
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(K... ids) {
		return perform( () -> (List<T>) entityPersister.multiLoad( ids, session, this ) );
	}

	public List<T> perform(Supplier<List<T>> executor) {
		CacheMode sessionCacheMode = session.getCacheMode();
		boolean cacheModeChanged = false;
		if ( cacheMode != null ) {
			// naive check for now...
			// todo : account for "conceptually equal"
			if ( cacheMode != sessionCacheMode ) {
				session.setCacheMode( cacheMode );
				cacheModeChanged = true;
			}
		}

		try {
			if ( graphSemantic != null ) {
				if ( rootGraph == null ) {
					throw new IllegalArgumentException( "Graph semantic specified, but no RootGraph was supplied" );
				}
				session.getLoadQueryInfluencers().getEffectiveEntityGraph().applyGraph( rootGraph, graphSemantic );
			}

			try {
				return executor.get();
			}
			finally {
				if ( graphSemantic != null ) {
					session.getLoadQueryInfluencers().getEffectiveEntityGraph().clear();
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
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(List<K> ids) {
		if ( ids.isEmpty() ) {
			return Collections.emptyList();
		}
		return perform( () -> (List<T>) entityPersister.multiLoad(
				ids.toArray( LoaderHelper.createTypedArray( ids.get( 0 ).getClass(), ids.size() ) ),
				session,
				this
		) );
	}
}
