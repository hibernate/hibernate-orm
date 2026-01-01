/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.OrderingMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class StatelessFindMultipleByKeyOperation<T> extends AbstractFindMultipleByKeyOperation {

	public static final MultiIdLoadOptions MULTI_ID_LOAD_OPTIONS = new MultiLoadOptions();
	@NonNull
	private final StatelessLoadAccessContext loadAccessContext;

	public StatelessFindMultipleByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull StatelessLoadAccessContext loadAccessContext,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@NonNull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		super( entityDescriptor,
				defaultLockOptions, defaultCacheMode, defaultReadOnly,
				sessionFactory, findOptions );
		this.loadAccessContext = loadAccessContext;
	}

	public List<T> performFind(
			List<?> keys,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<T> rootGraph) {
		// todo (jpa4) : allow for loading from cache

		for ( Object key : keys ) {
			if ( key == null ) {
				throw new IllegalArgumentException( "Null key" );
			}
		}

		if ( getKeyType() == KeyType.NATURAL ) {
			return findByNaturalIds( keys, graphSemantic, rootGraph );
		}
		else {
			return findByIds( keys, graphSemantic, rootGraph );
		}
	}

	private List<T> findByNaturalIds(
			List<?> keys,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph) {
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	private List<T> findByIds(
			List<?> keys,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph) {
		var session = loadAccessContext.getStatelessSession();
		return withOptions( session, graphSemantic, rootGraph, () -> {
			final var results = getEntityDescriptor().multiLoad( keys.toArray(), session, multiLoadOptions( getLockMode() ) );
			//noinspection unchecked
			return (List<T>) results;
		} );
	}

	private MultiIdLoadOptions multiLoadOptions(LockMode lockMode) {
		return lockMode == null ? MULTI_ID_LOAD_OPTIONS : new MultiLoadOptions( lockMode );
	}

	private List<T> withOptions(
			StatelessSessionImplementor session,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		final var influencers = session.getLoadQueryInfluencers();
		final var fetchProfiles = influencers.adjustFetchProfiles( getDisabledFetchProfiles(), getEnabledFetchProfiles() );
		final var effectiveEntityGraph = rootGraph == null
				? null
				: influencers.applyEntityGraph( rootGraph, graphSemantic );

		try {
			return action.get();
		}
		finally {
			if ( effectiveEntityGraph != null ) {
				effectiveEntityGraph.clear();
			}
			influencers.setEnabledFetchProfileNames( fetchProfiles );
		}
	}

	private static final class MultiLoadOptions implements MultiIdLoadOptions {
		private final  LockOptions lockOptions;

		private MultiLoadOptions() {
			this.lockOptions = null;
		}

		private MultiLoadOptions(LockMode lockMode) {
			this.lockOptions = new LockOptions( lockMode );
		}

		@Override
		public SessionCheckMode getSessionCheckMode() {
			return SessionCheckMode.DISABLED;
		}

		@Override
		public boolean isSecondLevelCacheCheckingEnabled() {
			return true;
		}

		@Override
		public Boolean getReadOnly(SessionImplementor session) {
			return null;
		}

		@Override
		public RemovalsMode getRemovalsMode() {
			return RemovalsMode.REPLACE;
		}

		@Override
		public OrderingMode getOrderingMode() {
			return OrderingMode.ORDERED;
		}

		@Override
		public LockOptions getLockOptions() {
			return lockOptions;
		}

		@Override
		public Integer getBatchSize() {
			return null;
		}
	}
}
