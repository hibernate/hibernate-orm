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
import org.hibernate.CacheMode;
import org.hibernate.EnabledFetchProfile;
import org.hibernate.KeyType;
import org.hibernate.FindMultipleOption;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Timeouts;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// Support for loading a single entity by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see org.hibernate.Session#find
/// @see KeyType
///
/// @author Steve Ebersole
public class FindByKeyOperation<T> implements NaturalIdLoader.Options {
	private final EntityPersister entityDescriptor;

	private KeyType keyType = KeyType.IDENTIFIER;

	private CacheStoreMode cacheStoreMode;
	private CacheRetrieveMode cacheRetrieveMode;

	private LockMode lockMode;
	private Locking.Scope lockScope;
	private Locking.FollowOn lockFollowOn;
	private Timeout lockTimeout = WAIT_FOREVER;

	private ReadOnlyMode readOnlyMode;

	private RootGraphImplementor<?> rootGraph;
	private GraphSemantic graphSemantic;

	private Set<String> enabledFetchProfiles;

	private NaturalIdSynchronization naturalIdSynchronization;

	public FindByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<?> rootGraph,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@NonNull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		this.entityDescriptor = entityDescriptor;

		this.graphSemantic = graphSemantic;
		this.rootGraph = rootGraph;

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
			else if ( option instanceof FindMultipleOption findMultipleOption ) {
				throw new IllegalArgumentException( "Option '" + findMultipleOption + "' can only be used in 'findMultiple()'" );
			}
		}
	}

	private void enabledFetchProfile(String profileName) {
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
	}

	public T performFind(Object key, LoadAccessContext loadAccessContext) {
		if ( keyType == KeyType.NATURAL ) {
			return findByNaturalId( key, loadAccessContext );
		}
		else {
			return findById( key, loadAccessContext );
		}

	}

	private T findByNaturalId(Object key, LoadAccessContext loadAccessContext) {
		final NaturalIdMapping naturalIdMapping = entityDescriptor.requireNaturalIdMapping();
		final SessionImplementor session = loadAccessContext.getSession();

		performAnyNeededCrossReferenceSynchronizations(
				naturalIdSynchronization != NaturalIdSynchronization.DISABLED,
				entityDescriptor,
				session
		);

		final var normalizedKey = naturalIdMapping.normalizeInput( key );

		final Object cachedResolution = getCachedNaturalIdResolution( normalizedKey, loadAccessContext );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}

		if ( cachedResolution != null ) {
			return findById( cachedResolution, loadAccessContext );
		}

		return withOptions( loadAccessContext, () -> {
			@SuppressWarnings("unchecked")
			final T loaded = (T) entityDescriptor.getNaturalIdLoader()
							.load( normalizedKey, this, session );
			if ( loaded != null ) {
				final var persistenceContext = session.getPersistenceContextInternal();
				final var lazyInitializer = HibernateProxy.extractLazyInitializer( loaded );
				final var entity = lazyInitializer != null ? lazyInitializer.getImplementation() : loaded;
				final var entry = persistenceContext.getEntry( entity );
				assert entry != null;
				if ( entry.getStatus() == Status.DELETED ) {
					return null;
				}
			}
			return loaded;
		} );
	}

	private T withOptions(LoadAccessContext loadAccessContext, Supplier<T> action) {
		final var session = loadAccessContext.getSession();
		final var influencers = session.getLoadQueryInfluencers();
		final var fetchProfiles = influencers.adjustFetchProfiles( null, enabledFetchProfiles );
		final var effectiveEntityGraph = rootGraph == null
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

	private Object getCachedNaturalIdResolution(
			Object normalizedNaturalIdValue,
			LoadAccessContext loadAccessContext) {
		loadAccessContext.checkOpenOrWaitingForAutoClose();
		loadAccessContext.pulseTransactionCoordinator();

		return loadAccessContext
				.getSession()
				.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.findCachedIdByNaturalId( normalizedNaturalIdValue, entityDescriptor );
	}

	private T findById(Object key, LoadAccessContext loadAccessContext) {
		return withOptions( loadAccessContext, () -> {
			final var session = loadAccessContext.getSession();
			Object result;
			try {
				result = loadAccessContext.load(
						LoadEventListener.GET,
						coerceId( key, session.getFactory() ),
						entityDescriptor.getEntityName(),
						makeLockOptions(),
						readOnlyMode == ReadOnlyMode.READ_ONLY
				);
			}
			catch (ObjectNotFoundException notFoundException) {
				// if session cache contains proxy for nonexisting object
				result = null;
			}
			initializeIfNecessary( result );
			//noinspection unchecked
			return (T) result;
		} );
	}

	private LockOptions makeLockOptions() {
		return Helper.makeLockOptions( lockMode, lockScope, lockTimeout, lockFollowOn );
	}

	// Used by Hibernate Reactive
	protected Object coerceId(Object id, SessionFactoryImplementor factory) {
		if ( factory.getSessionFactoryOptions().getJpaCompliance().isLoadByIdComplianceEnabled() ) {
			return id;
		}

		try {
			return entityDescriptor.getIdentifierMapping().getJavaType().coerce( id );
		}
		catch ( Exception e ) {
			throw new IllegalArgumentException( "Argument '" + id
												+ "' could not be converted to the identifier type of entity '"
												+ entityDescriptor.getEntityName() + "'"
												+ " [" + e.getMessage() + "]", e );
		}
	}

	private void initializeIfNecessary(Object result) {
		if ( result != null ) {
			final var lazyInitializer = extractLazyInitializer( result );
			if ( lazyInitializer != null ) {
				if ( lazyInitializer.isUninitialized() ) {
					lazyInitializer.initialize();
				}
			}
			else {
				final var enhancementMetadata = entityDescriptor.getBytecodeEnhancementMetadata();
				if ( enhancementMetadata.isEnhancedForLazyLoading()
					&& enhancementMetadata.extractLazyInterceptor( result )
							instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
					lazinessInterceptor.forceInitialize( result, null );
				}
			}
		}
	}

	@Override
	public LockMode getLockMode() {
		return lockMode;
	}

	@Override
	public Timeout getLockTimeout() {
		return lockTimeout;
	}

	@Override
	public Locking.Scope getLockScope() {
		return lockScope;
	}

	@Override
	public Locking.FollowOn getLockFollowOn() {
		return lockFollowOn;
	}
}
