/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.FindOption;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.EnabledFetchProfile;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;

/// Support for loading multiple entities (of a type) by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see org.hibernate.Session#findMultiple
/// @see KeyType
///
/// @author Steve Ebersole
public class FindMultipleByKeyOperation<T> implements MultiIdLoadOptions, MultiNaturalIdLoadOptions {
	private final EntityPersister entityDescriptor;

	private KeyType keyType = KeyType.IDENTIFIER;

	private BatchSize batchSize;
	private SessionCheckMode sessionCheckMode = SessionCheckMode.DISABLED;
	private RemovalsMode removalsMode = RemovalsMode.REPLACE;
	private OrderingMode orderingMode = OrderingMode.ORDERED;

	private CacheStoreMode cacheStoreMode;
	private CacheRetrieveMode cacheRetrieveMode;

	private LockMode lockMode;
	private Locking.Scope lockScope;
	private Locking.FollowOn lockFollowOn;
	private Timeout lockTimeout = WAIT_FOREVER;

	private ReadOnlyMode readOnlyMode;

	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	private NaturalIdSynchronization naturalIdSynchronization;

	@SuppressWarnings("PatternVariableHidesField")
	public FindMultipleByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@NonNull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		this.entityDescriptor = entityDescriptor;

		if ( defaultCacheMode != null ) {
			cacheStoreMode = defaultCacheMode.getJpaStoreMode();
			cacheRetrieveMode = defaultCacheMode.getJpaRetrieveMode();
		}

		if ( defaultLockOptions != null ) {
			lockMode = defaultLockOptions.getLockMode();
			lockScope = defaultLockOptions.getScope();
			lockTimeout = defaultLockOptions.getTimeout();
			lockFollowOn = defaultLockOptions.getFollowOnStrategy();
		}
		if ( lockTimeout == WAIT_FOREVER ) {
			final Object factoryTimeoutHint = sessionFactory.getProperties().get( HINT_SPEC_LOCK_TIMEOUT );
			if ( factoryTimeoutHint != null ) {
				lockTimeout = Timeouts.fromHintTimeout( factoryTimeoutHint );
			}
		}

		readOnlyMode = defaultReadOnly ? ReadOnlyMode.READ_ONLY : ReadOnlyMode.READ_WRITE;

		for ( FindOption option : findOptions ) {
			if ( option instanceof KeyType keyType ) {
				this.keyType = keyType;
			}
			else if (  option instanceof BatchSize batchSize ) {
				this.batchSize = batchSize;
			}
			else if ( option instanceof SessionCheckMode sessionCheckMode ) {
				this.sessionCheckMode = sessionCheckMode;
			}
			else if ( option instanceof RemovalsMode removalsMode ) {
				this.removalsMode = removalsMode;
			}
			else if ( option instanceof OrderingMode orderingMode ) {
				this.orderingMode = orderingMode;
			}
			else if ( option instanceof CacheStoreMode cacheStoreMode ) {
				this.cacheStoreMode = cacheStoreMode;
			}
			else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
				this.cacheRetrieveMode = cacheRetrieveMode;
			}
			else if ( option instanceof CacheMode cacheMode ) {
				this.cacheStoreMode = cacheMode.getJpaStoreMode();
				this.cacheRetrieveMode = cacheMode.getJpaRetrieveMode();
			}
			else if ( option instanceof LockModeType lockModeType ) {
				this.lockMode = LockModeTypeHelper.getLockMode( lockModeType );
			}
			else if ( option instanceof LockMode lockMode ) {
				this.lockMode = lockMode;
			}
			else if ( option instanceof Locking.Scope lockScope ) {
				this.lockScope = lockScope;
			}
			else if ( option instanceof PessimisticLockScope pessimisticLockScope ) {
				this.lockScope = Locking.Scope.fromJpaScope( pessimisticLockScope );
			}
			else if ( option instanceof Locking.FollowOn followOn ) {
				this.lockFollowOn = followOn;
			}
			else if ( option instanceof Timeout timeout ) {
				this.lockTimeout = timeout;
			}
			else if ( option instanceof ReadOnlyMode readOnlyMode) {
				this.readOnlyMode = readOnlyMode;
			}
			else if ( option instanceof EnabledFetchProfile enabledFetchProfile ) {
				this.enabledFetchProfile( enabledFetchProfile.profileName() );
			}
			else if ( option instanceof NaturalIdSynchronization naturalIdSynchronization ) {
				this.naturalIdSynchronization = naturalIdSynchronization;
			}
		}
	}

	private void enabledFetchProfile(String profileName) {
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
	}

	public List<T> performFind(
			List<?> keys,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<T> rootGraph,
			LoadAccessContext loadAccessContext) {
		// todo (natural-id-class) : these impls are temporary
		//		longer term, move the logic here as much of it can be shared
		return keyType == KeyType.NATURAL
				? findByNaturalIds( keys, graphSemantic, rootGraph, loadAccessContext )
				: findByIds( keys, graphSemantic, rootGraph, loadAccessContext );
	}

	private List<T> findByNaturalIds(List<?> keys, GraphSemantic graphSemantic, RootGraphImplementor<T> rootGraph, LoadAccessContext loadAccessContext) {
		final var naturalIdMapping = entityDescriptor.requireNaturalIdMapping();
		final var session = loadAccessContext.getSession();

		performAnyNeededCrossReferenceSynchronizations(
				naturalIdSynchronization != NaturalIdSynchronization.DISABLED,
				entityDescriptor,
				session
		);

		return withOptions( loadAccessContext, graphSemantic, rootGraph, () -> {
			// normalize the incoming natural-id values and get them in array form as needed
			// by MultiNaturalIdLoader
			final int size = keys.size();
			final var naturalIds = new Object[size];
			for ( int i = 0; i < size; i++ ) {
				naturalIds[i] = naturalIdMapping.normalizeInput( keys.get( i ) );
			}
			//noinspection unchecked
			return (List<T>)
					entityDescriptor.getMultiNaturalIdLoader()
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
		final var fetchProfiles =
				influencers.adjustFetchProfiles( disabledFetchProfiles, enabledFetchProfiles );
		final var effectiveEntityGraph =
				rootGraph == null
						? null
						: influencers.applyEntityGraph( rootGraph, graphSemantic );

		final var readOnly = session.isDefaultReadOnly();
		session.setDefaultReadOnly( readOnlyMode == ReadOnlyMode.READ_ONLY );

		final var cacheMode = session.getCacheMode();
		session.setCacheMode( CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode ) );

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

//	private Object getCachedNaturalIdResolution(
//			Object normalizedNaturalIdValue,
//			LoadAccessContext loadAccessContext) {
//		loadAccessContext.checkOpenOrWaitingForAutoClose();
//		loadAccessContext.pulseTransactionCoordinator();
//
//		return loadAccessContext
//				.getSession()
//				.getPersistenceContextInternal()
//				.getNaturalIdResolutions()
//				.findCachedIdByNaturalId( normalizedNaturalIdValue, entityDescriptor );
//	}

	private List<T> findByIds(List<?> keys, GraphSemantic graphSemantic, RootGraphImplementor<T> rootGraph, LoadAccessContext loadAccessContext) {
		final var ids = keys.toArray( new Object[0] );
		//noinspection unchecked
		return withOptions( loadAccessContext, graphSemantic, rootGraph,
				() -> (List<T>) entityDescriptor.multiLoad( ids, loadAccessContext.getSession(), this ) );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MultiIdLoadOptions & MultiNaturalIdLoadOptions

	@Override
	public SessionCheckMode getSessionCheckMode() {
		return sessionCheckMode;
	}

	@Override
	public boolean isSecondLevelCacheCheckingEnabled() {
		return cacheRetrieveMode == CacheRetrieveMode.USE;
	}

	@Override
	public Boolean getReadOnly(SessionImplementor session) {
		return readOnlyMode == null ? null : readOnlyMode == ReadOnlyMode.READ_ONLY;
	}

	@Override
	public RemovalsMode getRemovalsMode() {
		return removalsMode;
	}

	@Override
	public OrderingMode getOrderingMode() {
		return orderingMode;
	}

	@Override
	public LockOptions getLockOptions() {
		return Helper.makeLockOptions( lockMode, lockScope, lockTimeout, lockFollowOn );
	}

	@Override
	public Integer getBatchSize() {
		return batchSize == null ? null : batchSize.batchSize();
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/// Temporarily defined full constructor in support of
	/// [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess].
	///
	/// @deprecated [org.hibernate.MultiIdentifierLoadAccess] and [org.hibernate.MultiIdentifierLoadAccess]
	/// are both also deprecated.
	@Deprecated
	public FindMultipleByKeyOperation(
			EntityPersister entityDescriptor,
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
		if ( cacheMode == null ) {
			cacheMode = CacheMode.NORMAL;
		}
		if ( lockOptions == null ) {
			lockOptions = LockOptions.NONE;
		}
		this.entityDescriptor = entityDescriptor;
		this.keyType = keyType;
		this.batchSize = batchSize;
		this.sessionCheckMode = sessionCheckMode;
		this.removalsMode = removalsMode;
		this.orderingMode = orderingMode;
		this.cacheStoreMode = cacheMode.getJpaStoreMode();
		this.cacheRetrieveMode = cacheMode.getJpaRetrieveMode();
		this.lockMode = lockOptions.getLockMode();
		this.lockScope = lockOptions.getScope();
		this.lockFollowOn = lockOptions.getFollowOnStrategy();
		this.lockTimeout = lockOptions.getTimeout();
		this.readOnlyMode = readOnlyMode;
		this.enabledFetchProfiles = enabledFetchProfiles;
		this.disabledFetchProfiles = disabledFetchProfiles;
		this.naturalIdSynchronization = naturalIdSynchronization;
	}
}
