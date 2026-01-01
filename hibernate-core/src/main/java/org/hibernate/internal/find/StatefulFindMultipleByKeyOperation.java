/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.KeyType;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;

/// Support for loading multiple entities (of a type) by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see org.hibernate.Session#findMultiple
/// @see KeyType
///
/// @author Steve Ebersole
public class StatefulFindMultipleByKeyOperation<T> extends AbstractFindMultipleByKeyOperation {
	private final LoadAccessContext loadAccessContext;

	public StatefulFindMultipleByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull LoadAccessContext loadAccessContext,
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
			List<Object> keys,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<T> rootGraph) {
		// todo (natural-id-class) : these impls are temporary
		//		longer term, move the logic here as much of it can be shared
		if ( getKeyType() == KeyType.NATURAL ) {
			return findByNaturalIds( keys, graphSemantic, rootGraph, loadAccessContext );
		}
		else {
			return findByIds( keys, graphSemantic, rootGraph, loadAccessContext );
		}
	}

	private List<T> findByNaturalIds(List<Object> keys, GraphSemantic graphSemantic, RootGraphImplementor<T> rootGraph, LoadAccessContext loadAccessContext) {
		final NaturalIdMapping naturalIdMapping = getEntityDescriptor().requireNaturalIdMapping();
		final SessionImplementor session = loadAccessContext.getSession();

		performAnyNeededCrossReferenceSynchronizations(
				getNaturalIdSynchronization() != NaturalIdSynchronization.DISABLED,
				getEntityDescriptor(),
				session
		);

		return withOptions( loadAccessContext, graphSemantic, rootGraph, () -> {
			// normalize the incoming natural-id values and get them in array form as needed
			// by MultiNaturalIdLoader
			final Object[] naturalIds = new Object[keys.size()];
			for ( int i = 0; i < keys.size(); i++ ) {
				final Object key = keys.get( i );
				naturalIds[i] = naturalIdMapping.normalizeInput( key );
			}

			//noinspection unchecked
			return (List<T>)getEntityDescriptor().getMultiNaturalIdLoader()
					.multiLoad( naturalIds, this, session );
		} );
	}

	private List<T> withOptions(
			LoadAccessContext loadAccessContext,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		final var session = loadAccessContext.getSession();
		final var influencers = session.getLoadQueryInfluencers();
		final var fetchProfiles = influencers.adjustFetchProfiles( getDisabledFetchProfiles(), getEnabledFetchProfiles() );
		final var effectiveEntityGraph = rootGraph == null
				? null
				: influencers.applyEntityGraph( rootGraph, graphSemantic );

		final var readOnly = session.isDefaultReadOnly();
		session.setDefaultReadOnly( getReadOnlyMode() == ReadOnlyMode.READ_ONLY );

		final var cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.fromJpaModes( getCacheRetrieveMode(), getCacheStoreMode() ) );

		try {
			return action.get();
		}
		finally {
			loadAccessContext.delayedAfterCompletion();
			if ( effectiveEntityGraph != null ) {
				effectiveEntityGraph.clear();
			}
			influencers.setEnabledFetchProfileNames( fetchProfiles );
			session.setDefaultReadOnly( readOnly );
			session.setCacheMode( cacheMode );
		}
	}

	private Object getCachedNaturalIdResolution(
			Object normalizedNaturalIdValue,
			LoadAccessContext loadAccessContext) {
		loadAccessContext.checkOpenOrWaitingForAutoClose();
		loadAccessContext.pulseTransactionCoordinator();

		return loadAccessContext
				.getSession()
				.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.findCachedIdByNaturalId( normalizedNaturalIdValue, getEntityDescriptor() );
	}

	private List<T> findByIds(List<Object> keys, GraphSemantic graphSemantic, RootGraphImplementor<T> rootGraph, LoadAccessContext loadAccessContext) {
		final Object[] ids = keys.toArray( new Object[0] );
		//noinspection unchecked
		return withOptions( loadAccessContext, graphSemantic, rootGraph,
				() -> (List<T>) getEntityDescriptor().multiLoad( ids, loadAccessContext.getSession(), this ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Temporarily defined full constructor in support of
	/// [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess].
	///
	/// @deprecated [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess]
	/// are both also deprecated.
	@Deprecated
	public StatefulFindMultipleByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull LoadAccessContext loadAccessContext,
			KeyType keyType,
			BatchSize batchSize,
			SessionCheckMode sessionCheckMode,
			RemovalsMode removalsMode,
			OrderingMode orderingMode,
			CacheMode cacheMode,
			LockOptions lockOptions,
			ReadOnlyMode readOnlyMode,
			Set<String> enabledFetchProfiles,
			Set<String> disabledFetchProfiles,
			NaturalIdSynchronization naturalIdSynchronization) {
		super(
				entityDescriptor,
				keyType,
				batchSize,
				sessionCheckMode,
				removalsMode,
				orderingMode,
				cacheMode,
				lockOptions,
				readOnlyMode,
				enabledFetchProfiles,
				disabledFetchProfiles,
				naturalIdSynchronization
		);
		this.loadAccessContext = loadAccessContext;
	}
}
