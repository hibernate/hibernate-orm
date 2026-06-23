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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EnabledFetchProfile;
import org.hibernate.FindMultipleOption;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;
import static org.hibernate.internal.find.Helper.checkTransactionNeededForLock;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;

/// Base support for loading multiple entities (of a type) by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see KeyType
///
/// @author Steve Ebersole
public abstract class AbstractFindMultipleByKeyOperation<T> implements MultiIdLoadOptions, MultiNaturalIdLoadOptions {
	private final EntityPersister entityDescriptor;

	private KeyType keyType = KeyType.IDENTIFIER;

	private FindMultipleOption.BatchSize batchSize;
	private FindMultipleOption.SessionCheckMode sessionCheckMode = FindMultipleOption.SessionCheckMode.ENABLED;
	private FindMultipleOption.RemovalsMode removalsMode = FindMultipleOption.RemovalsMode.REPLACE;
	private FindMultipleOption.OrderingMode orderingMode = FindMultipleOption.OrderingMode.ORDERED;

	private CacheStoreMode cacheStoreMode;
	private CacheRetrieveMode cacheRetrieveMode;
	private boolean refreshSession;

	private LockMode lockMode;
	private PessimisticLockScope lockScope;
	private Locking.FollowOn lockFollowOn;
	private Timeout lockTimeout = WAIT_FOREVER;

	private ReadOnlyMode readOnlyMode;

	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	private NaturalIdSynchronization naturalIdSynchronization;

	@SuppressWarnings({ "PatternVariableHidesField", "removal" })
	public AbstractFindMultipleByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@Nonnull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		this.entityDescriptor = entityDescriptor;

		if ( defaultCacheMode != null ) {
			setCacheMode( defaultCacheMode );
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

		if ( findOptions != null ) {
			for ( var option : findOptions ) {
				if ( option instanceof KeyType keyType ) {
					this.keyType = keyType;
				}
				else if ( option instanceof org.hibernate.BatchSize batchSize ) {
					this.batchSize = new FindMultipleOption.BatchSize( batchSize.batchSize() );
				}
				else if ( option instanceof FindMultipleOption.BatchSize batchSize ) {
					this.batchSize = batchSize;
				}
				else if ( option instanceof FindMultipleOption.SessionCheckMode sessionCheckMode ) {
					this.sessionCheckMode = sessionCheckMode;
				}
				else if ( option instanceof FindMultipleOption.RemovalsMode removalsMode ) {
					this.removalsMode = removalsMode;
				}
				else if ( option instanceof FindMultipleOption.OrderingMode orderingMode ) {
					this.orderingMode = orderingMode;
				}
				else if ( option instanceof CacheStoreMode cacheStoreMode ) {
					this.cacheStoreMode = cacheStoreMode;
					this.refreshSession = false;
				}
				else if ( option instanceof CacheRetrieveMode cacheRetrieveMode ) {
					this.cacheRetrieveMode = cacheRetrieveMode;
					this.refreshSession = false;
				}
				else if ( option instanceof CacheMode cacheMode ) {
					setCacheMode( cacheMode );
				}
				else if ( option instanceof LockModeType lockModeType ) {
					this.lockMode = LockModeTypeHelper.getLockMode( lockModeType );
				}
				else if ( option instanceof LockMode lockMode ) {
					this.lockMode = lockMode;
				}
				else if ( option instanceof PessimisticLockScope pessimisticLockScope ) {
					this.lockScope = pessimisticLockScope;
				}
				else if ( option instanceof Locking.FollowOn followOn ) {
					this.lockFollowOn = followOn;
				}
				else if ( option instanceof Timeout timeout ) {
					this.lockTimeout = timeout;
				}
				else if ( option instanceof ReadOnlyMode readOnlyMode ) {
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
	}

	private void enabledFetchProfile(String profileName) {
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
	}

	protected void checkKeys(List<?> keys) {
		if ( keys == null ) {
			throw new IllegalArgumentException( "Null keys" );
		}
		for ( Object key : keys ) {
			if ( key == null ) {
				throw new IllegalArgumentException( "Null key" );
			}
		}
	}

	protected void checkFindRequirements(List<?> keys, SharedSessionContractImplementor session) {
		checkKeys( keys );
		checkTransactionNeededForLock( session, getLockMode() );
	}

	public List<T> performFind(
			List<?> keys,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<T> rootGraph) {
		final var session = getSession();
		checkFindRequirements( keys, session );
		return getKeyType() == KeyType.NATURAL
				? findByNaturalIds( keys, session, graphSemantic, rootGraph )
				: findByIds( keys, session, graphSemantic, rootGraph );
	}

	protected abstract SharedSessionContractImplementor getSession();

	private void setCacheMode(CacheMode cacheMode) {
		cacheStoreMode = cacheMode.getJpaStoreMode();
		cacheRetrieveMode = cacheMode.getJpaRetrieveMode();
		refreshSession = cacheMode == CacheMode.REFRESH_SESSION;
	}

	protected CacheMode getCacheMode() {
		final var cacheMode = CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode );
		return refreshSession && cacheMode == CacheMode.REFRESH
				? CacheMode.REFRESH_SESSION
				: cacheMode;
	}

	@Override
	public boolean isRefreshSession() {
		return getCacheMode() == CacheMode.REFRESH_SESSION;
	}

	public EntityPersister getEntityDescriptor() {
		return entityDescriptor;
	}

	public KeyType getKeyType() {
		return keyType;
	}

	public CacheStoreMode getCacheStoreMode() {
		return cacheStoreMode;
	}

	public CacheRetrieveMode getCacheRetrieveMode() {
		return cacheRetrieveMode;
	}

	public LockMode getLockMode() {
		return lockMode;
	}

	public PessimisticLockScope getLockScope() {
		return lockScope;
	}

	public Locking.FollowOn getLockFollowOn() {
		return lockFollowOn;
	}

	public Timeout getLockTimeout() {
		return lockTimeout;
	}

	public ReadOnlyMode getReadOnlyMode() {
		return readOnlyMode;
	}

	public Set<String> getEnabledFetchProfiles() {
		return enabledFetchProfiles;
	}

	public Set<String> getDisabledFetchProfiles() {
		return disabledFetchProfiles;
	}

	public NaturalIdSynchronization getNaturalIdSynchronization() {
		return naturalIdSynchronization;
	}

	private List<T> findByIds(
			List<?> keys,
			SharedSessionContractImplementor session,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph) {
		final var ids = Helper.coerceIds( getEntityDescriptor(), keys, session );
		return withOptions( session, graphSemantic, rootGraph, () -> {
			// todo (jpa4) : make sure loading from cache happens inside here
			final var results = getEntityDescriptor().multiLoad( ids, session, this );
			//noinspection unchecked
			return (List<T>) results;
		} );
	}

	protected List<T> findByNaturalIds(
			List<?> keys,
			SharedSessionContractImplementor session,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph) {
		final var entityDescriptor = getEntityDescriptor();
		final var naturalIdMapping = entityDescriptor.requireNaturalIdMapping();
		performAnyNeededCrossReferenceSynchronizations(
				getNaturalIdSynchronization() != NaturalIdSynchronization.DISABLED,
				entityDescriptor,
				session
		);
		return withOptions( session, graphSemantic, rootGraph, () -> {
			// normalize the incoming natural-id values and get them in array form as needed
			// by MultiNaturalIdLoader
			final Object[] naturalIds = new Object[keys.size()];
			for ( int i = 0; i < keys.size(); i++ ) {
				naturalIds[i] = naturalIdMapping.normalizeInput( keys.get( i ) );
			}
			//noinspection unchecked
			return (List<T>) entityDescriptor.getMultiNaturalIdLoader()
					.multiLoad( naturalIds, this, session );
		} );
	}

	protected List<T> withLoadQueryInfluencers(
			SharedSessionContractImplementor session,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action) {
		final var influencers = session.getLoadQueryInfluencers();
		final var fetchProfiles =
				influencers.adjustFetchProfiles( getDisabledFetchProfiles(), getEnabledFetchProfiles() );
		final var effectiveEntityGraph =
				rootGraph == null
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

	protected abstract List<T> withOptions(
			SharedSessionContractImplementor session,
			GraphSemantic graphSemantic,
			RootGraphImplementor<T> rootGraph,
			Supplier<List<T>> action);

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// MultiIdLoadOptions & MultiNaturalIdLoadOptions

	@Override
	public FindMultipleOption.SessionCheckMode getSessionCheckMode() {
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
	public FindMultipleOption.RemovalsMode getRemovalsMode() {
		return removalsMode;
	}

	@Override
	public FindMultipleOption.OrderingMode getOrderingMode() {
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
	public AbstractFindMultipleByKeyOperation(
			EntityPersister entityDescriptor,
			KeyType keyType,
			FindMultipleOption.BatchSize batchSize,
			FindMultipleOption.SessionCheckMode sessionCheckMode,
			FindMultipleOption.RemovalsMode removalsMode,
			FindMultipleOption.OrderingMode orderingMode,
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
		this.refreshSession = cacheMode == CacheMode.REFRESH_SESSION;
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
