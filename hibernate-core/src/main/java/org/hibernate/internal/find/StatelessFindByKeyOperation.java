/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.internal.CacheLoadHelper;
import org.hibernate.persister.entity.EntityPersister;

/// Support for loading a single entity by key by either
/// [id][org.hibernate.KeyType#IDENTIFIER] or
/// [natural id][org.hibernate.KeyType#NATURAL]
/// from a [stateless session][org.hibernate.StatelessSession].
///
/// @see org.hibernate.StatelessSession#find
/// @see org.hibernate.KeyType
///
/// @author Steve Ebersole
public class StatelessFindByKeyOperation<T> extends AbstractFindByKeyOperation<T> {
	private final StatelessLoadAccessContext loadAccessContext;

	public StatelessFindByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatelessLoadAccessContext loadAccessContext,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<?> rootGraph,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@Nonnull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		super( entityDescriptor, graphSemantic, rootGraph,
				defaultLockOptions, defaultCacheMode, defaultReadOnly,
				sessionFactory, findOptions );
		this.loadAccessContext = loadAccessContext;
	}

	private StatelessSessionImplementor getSession() {
		return loadAccessContext.getStatelessSession();
	}

	@Override
	protected SharedSessionContractImplementor getEntityHandler() {
		return getSession();
	}

	@Override
	protected CacheMode getCacheModeForOptions() {
		return CacheMode.fromJpaModes( getCacheRetrieveMode(), getCacheStoreMode() );
	}

	@Override
	protected void afterOptions() {
		final var persistenceContext = getSession().getPersistenceContextInternal();
		if ( persistenceContext.isLoadFinished() ) {
			persistenceContext.clear();
		}
	}

	@Override
	protected T findById(Object key) {
		final Object keyToLoad =
				Helper.coerceId( getEntityDescriptor(), key,
						getSession().getFactory() );

		final Object cachedEntity = loadFromSecondLevelCache( key, loadAccessContext );
		if ( cachedEntity != null ) {
			final var temporaryPersistenceContext = getSession().getPersistenceContext();
			if ( temporaryPersistenceContext.isLoadFinished() ) {
				temporaryPersistenceContext.clear();
			}
			//noinspection unchecked
			return (T) cachedEntity;
		}

		return withOptions( () -> {
			final Object result = getEntityDescriptor().load(
					keyToLoad,
					null,
					getNullSafeLockMode(),
					getSession()
			);
			//noinspection unchecked
			return (T) result;
		} );
	}

	private Object loadFromSecondLevelCache(
			Object key,
			StatelessLoadAccessContext context) {
		return CacheLoadHelper.loadFromSecondLevelCache(
				context.getStatelessSession(),
				null,
				getNullSafeLockMode(),
				getEntityDescriptor(),
				new EntityKey( key, getEntityDescriptor() )
		);
	}

	private LockMode getNullSafeLockMode() {
		return getLockMode() == null ? LockMode.NONE : getLockMode();
	}
}
