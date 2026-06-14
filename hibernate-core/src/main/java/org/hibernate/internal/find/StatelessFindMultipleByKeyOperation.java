/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.KeyType;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Steve Ebersole
 */
public class StatelessFindMultipleByKeyOperation<T> extends AbstractFindMultipleByKeyOperation<T> {

	@Nonnull
	private final StatelessLoadAccessContext loadAccessContext;

	public StatelessFindMultipleByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatelessLoadAccessContext loadAccessContext,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@Nonnull SessionFactoryImplementor sessionFactory,
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
		final var session = loadAccessContext.getStatelessSession();
		checkFindRequirements( keys, session );
		return getKeyType() == KeyType.NATURAL
				? findByNaturalIds( keys, session, graphSemantic, rootGraph )
				: findByIds( keys, graphSemantic, rootGraph );
	}

	private List<T> findByIds(
			List<?> keys,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph) {
		var session = loadAccessContext.getStatelessSession();
		final var ids = Helper.coerceIds( getEntityDescriptor(), keys, session );
		return withOptions( session, graphSemantic, rootGraph, () -> {
			// todo (jpa4) : make sure loading from cache happens inside here
			final var results = getEntityDescriptor().multiLoad( ids, session, this );
			//noinspection unchecked
			return (List<T>) results;
		} );
	}

	@Override
	protected List<T> withOptions(
			SharedSessionContractImplementor sharedSession,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		final var session = (StatelessSessionImplementor) sharedSession;
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
}
