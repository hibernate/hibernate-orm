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
import org.hibernate.FindMultipleOption;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Locking;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Timeouts;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.internal.util.LockModeTypeHelper;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.Set;

import static org.hibernate.Timeouts.WAIT_FOREVER;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.jpa.SpecHints.HINT_SPEC_LOCK_TIMEOUT;

/// Base support for loading a single entity by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL]).
///
/// @see KeyType
///
/// @author Steve Ebersole
public abstract class AbstractFindByKeyOperation<T> implements FindByKeyOperation<T>, NaturalIdLoader.Options {
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

	public AbstractFindByKeyOperation(
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

	public ReadOnlyMode getReadOnlyMode() {
		return readOnlyMode;
	}

	public RootGraphImplementor<?> getRootGraph() {
		return rootGraph;
	}

	public GraphSemantic getGraphSemantic() {
		return graphSemantic;
	}

	public Set<String> getEnabledFetchProfiles() {
		return enabledFetchProfiles;
	}

	public NaturalIdSynchronization getNaturalIdSynchronization() {
		return naturalIdSynchronization;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NaturalIdLoader.Options

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

	protected boolean needsTransaction(LockMode lockMode) {
		return lockMode != null && lockMode.greaterThan( LockMode.NONE );
	}

	protected LockOptions makeLockOptions() {
		return Helper.makeLockOptions( getLockMode(), getLockScope(), getLockTimeout(), getLockFollowOn() );
	}

	protected void logIgnoringEntityNotFound(Object key) {
		if ( SESSION_LOGGER.isDebugEnabled() ) {
			SESSION_LOGGER.ignoringEntityNotFound( getEntityDescriptor().getEntityName(), key );
		}
	}
}
