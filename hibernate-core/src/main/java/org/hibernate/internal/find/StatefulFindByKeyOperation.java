/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FindOption;
import jakarta.persistence.TransactionRequiredException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.JDBCException;
import org.hibernate.KeyType;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.ReadOnlyMode;
import org.hibernate.Transaction;
import org.hibernate.TypeMismatchException;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Supplier;

import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// Support for loading a single entity by key (either [id][KeyType#IDENTIFIER] or [natural-id][KeyType#NATURAL])
/// from a [[org.hibernate.Session] stateful session].
///
/// @see org.hibernate.Session#find
/// @see KeyType
///
/// @author Steve Ebersole
public class StatefulFindByKeyOperation<T> extends AbstractFindByKeyOperation<T> {
	private final LoadAccessContext loadAccessContext;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// options

	public StatefulFindByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull LoadAccessContext loadAccessContext,
			@Nullable GraphSemantic graphSemantic,
			@Nullable RootGraphImplementor<?> rootGraph,
			@Nullable LockOptions defaultLockOptions,
			@Nullable CacheMode defaultCacheMode,
			boolean defaultReadOnly,
			@NonNull SessionFactoryImplementor sessionFactory,
			FindOption... findOptions) {
		super( entityDescriptor, graphSemantic, rootGraph,
				defaultLockOptions, defaultCacheMode, defaultReadOnly,
				sessionFactory, findOptions );
		this.loadAccessContext = loadAccessContext;
	}

	@Override
	public T performFind(Object key) {
		if ( needsTransaction( getLockMode() ) ) {
			if ( !loadAccessContext.getSession().isTransactionInProgress() ) {
				throw new TransactionRequiredException( String.format( Locale.ROOT,
						"Transaction required to load entity (%s#%s) with lock-mode (%s)",
						getEntityDescriptor().getEntityName(),
						key,
						getLockMode()
				) );
			}
		}

		return withExceptionHandling( key, makeLockOptions(), () -> {
			if ( getKeyType() == KeyType.NATURAL ) {
				return findByNaturalId( key );
			}
			else {
				return findById( key );
			}
		} );
	}

	private T withExceptionHandling(
			Object key,
			LockOptions lockOptions,
			Supplier<T> action) {
		try {
			return action.get();
		}
		catch ( FetchNotFoundException e ) {
			// This may happen if the entity has an association mapped with
			// @NotFound(action = NotFoundAction.EXCEPTION) and this associated
			// entity is not found
			throw e;
		}
		catch ( EntityFilterException e ) {
			// This may happen if the entity has an association which is
			// filtered by a FilterDef and this associated entity is not found
			throw e;
		}
		catch ( EntityNotFoundException e ) {
			// We swallow other sorts of EntityNotFoundException and return null
			// For example, DefaultLoadEventListener.proxyImplementation() throws
			// EntityNotFoundException if there's an existing proxy in the session,
			// but the underlying database row has been deleted (see HHH-7861)
			logIgnoringEntityNotFound( key );
			return null;
		}
		catch ( ObjectDeletedException e ) {
			// the spec is silent about people doing remove() find() on the same PC
			return null;
		}
		catch ( ObjectNotFoundException e ) {
			// should not happen on the entity itself with get
			// TODO: in fact this will occur instead of EntityNotFoundException
			//       when using StandardEntityNotFoundDelegate, so probably we
			//       should return null here, as we do above
			throw new IllegalArgumentException( e.getMessage(), e );
		}
		catch (MappingException | TypeMismatchException | ClassCastException e ) {
			throw getExceptionConverter().convert( new IllegalArgumentException( e.getMessage(), e ) );
		}
		catch (LockTimeoutException | jakarta.persistence.LockTimeoutException e) {
			throw e;
		}
		catch ( JDBCException e ) {
			if ( accessTransaction().isActive() ) {
				if ( accessTransaction().getRollbackOnly() ) {
					// Assume situation HHH-12472 running on WildFly
					// Just log the exception and return null
					SESSION_LOGGER.jdbcExceptionThrownWithTransactionRolledBack( e );
					return null;
				}
			}
			throw getExceptionConverter().convert( e, lockOptions );
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e, lockOptions );
		}
	}

	protected Transaction accessTransaction() {
		return loadAccessContext.getSession().accessTransaction();
	}

	protected ExceptionConverter getExceptionConverter() {
		return loadAccessContext.getSession().getExceptionConverter();
	}


	private T findByNaturalId(Object key) {
		final SessionImplementor session = loadAccessContext.getSession();

		performAnyNeededCrossReferenceSynchronizations(
				getNaturalIdSynchronization() != NaturalIdSynchronization.DISABLED,
				getEntityDescriptor(),
				session
		);

		final var normalizedKey = Helper.coerceNaturalId( getEntityDescriptor(), key );

		final Object cachedResolution = getCachedNaturalIdResolution( normalizedKey, loadAccessContext );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}

		if ( cachedResolution != null ) {
			return findById( cachedResolution );
		}

		return withOptions( loadAccessContext, () -> {
			@SuppressWarnings("unchecked")
			final T loaded = (T) getEntityDescriptor().getNaturalIdLoader()
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

		final var sessionCacheMode = session.getCacheMode();
		final var cacheMode = CacheMode.fromJpaModes( getCacheRetrieveMode(), getCacheStoreMode() );
		boolean cacheModeChanged = false;
		try {
			if ( cacheMode != null ) {
				if ( cacheMode != sessionCacheMode ) {
					session.setCacheMode( cacheMode );
					cacheModeChanged = true;
				}
			}

			final var influencers = session.getLoadQueryInfluencers();
			HashSet<String> fetchProfiles = null;
			EffectiveEntityGraph effectiveEntityGraph = null;

			try {
				fetchProfiles = influencers.adjustFetchProfiles( null, getEnabledFetchProfiles() );
				effectiveEntityGraph = getRootGraph() == null
						? null
						: influencers.applyEntityGraph( getRootGraph(), getGraphSemantic() );

				return action.get();
			}
			finally {
				if ( effectiveEntityGraph != null ) {
					effectiveEntityGraph.clear();
				}
				if ( fetchProfiles != null ) {
					influencers.setEnabledFetchProfileNames( fetchProfiles );
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

	private T findById(Object key) {
		return withOptions( loadAccessContext, () -> {
			final var session = loadAccessContext.getSession();

			Object result;
			try {
				result = loadAccessContext.load(
						LoadEventListener.GET,
						coerceId( key, session.getFactory() ),
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
