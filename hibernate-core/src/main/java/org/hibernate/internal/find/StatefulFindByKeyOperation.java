/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.FindOption;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.LockOptions;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// Support for loading a single entity by key by either
/// [id][org.hibernate.KeyType#IDENTIFIER] or
/// [natural id][org.hibernate.KeyType#NATURAL]
/// from a [stateful session][org.hibernate.Session].
///
/// @see org.hibernate.Session#find
/// @see org.hibernate.KeyType
///
/// @author Steve Ebersole
public class StatefulFindByKeyOperation<T> extends AbstractFindByKeyOperation<T> {
	private final StatefulLoadAccessContext loadAccessContext;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// options

	public StatefulFindByKeyOperation(
			@Nonnull EntityPersister entityDescriptor,
			@Nonnull StatefulLoadAccessContext loadAccessContext,
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

	private SessionImplementor getSession() {
		return loadAccessContext.getSession();
	}

	@Override
	protected SharedSessionContractImplementor getEntityHandler() {
		return getSession();
	}

	@Override
	protected T handleNaturalIdLoadResult(T loaded) {
		if ( loaded != null ) {
			final var lazyInitializer = extractLazyInitializer( loaded );
			final var entity = lazyInitializer != null ? lazyInitializer.getImplementation() : loaded;
			final var entry = getSession().getPersistenceContextInternal().getEntry( entity );
			assert entry != null;
			if ( entry.getStatus() == Status.DELETED ) {
				return null;
			}
		}
		return loaded;
	}

	@Override
	protected void beforeCachedNaturalIdResolution() {
		loadAccessContext.checkOpenOrWaitingForAutoClose();
		loadAccessContext.pulseTransactionCoordinator();
	}

	@Override
	protected T findById(Object key) {
		return withOptions( () -> {
			Object result;
			try {
				result = loadAccessContext.load(
						LoadEventListener.GET,
						coerceId( key, getSession().getFactory() ),
						getEntityDescriptor().getEntityName(),
						makeLockOptions(),
						getReadOnlyMode() == ReadOnlyMode.READ_ONLY
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

	// Used by Hibernate Reactive
	protected Object coerceId(Object id, SessionFactoryImplementor factory) {
		return Helper.coerceId( getEntityDescriptor(), id, factory );
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
				final var enhancementMetadata = getEntityDescriptor().getBytecodeEnhancementMetadata();
				if ( enhancementMetadata.isEnhancedForLazyLoading()
					&& enhancementMetadata.extractLazyInterceptor( result )
							instanceof EnhancementAsProxyLazinessInterceptor lazinessInterceptor ) {
					lazinessInterceptor.forceInitialize( result, null );
				}
			}
		}
	}
}
