/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FindOption;
import jakarta.persistence.TransactionRequiredException;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.CacheMode;
import org.hibernate.EntityFilterException;
import org.hibernate.FetchNotFoundException;
import org.hibernate.JDBCException;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.ObjectDeletedException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.TypeMismatchException;
import org.hibernate.engine.spi.EffectiveEntityGraph;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.ExceptionConverter;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.StatelessSessionImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.loader.internal.CacheLoadHelper;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.Locale;
import java.util.function.Supplier;

import static org.hibernate.engine.spi.NaturalIdResolutions.INVALID_NATURAL_ID_REFERENCE;
import static org.hibernate.internal.NaturalIdHelper.performAnyNeededCrossReferenceSynchronizations;
import static org.hibernate.internal.SessionLogging.SESSION_LOGGER;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unchecked")
public class StatelessFindByKeyOperation<T> extends AbstractFindByKeyOperation<T> {
	private final StatelessLoadAccessContext loadAccessContext;

	public StatelessFindByKeyOperation(
			@NonNull EntityPersister entityDescriptor,
			@NonNull StatelessLoadAccessContext loadAccessContext,
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
			if ( !loadAccessContext.getStatelessSession().isTransactionInProgress() ) {
				throw new TransactionRequiredException( String.format( Locale.ROOT,
						"Transaction required to load entity (%s#%s) with lock-mode (%s)",
						getEntityDescriptor().getEntityName(),
						key,
						getLockMode()
				) );
			}
		}

		try {
			if ( getKeyType() == KeyType.NATURAL ) {
				return findByNaturalId( key );
			}
			else {
				return findById( key );
			}
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
		catch ( JDBCException e ) {
			if ( accessTransaction().isActive() && accessTransaction().getRollbackOnly() ) {
				// Assume situation HHH-12472 running on WildFly
				// Just log the exception and return null
				SESSION_LOGGER.jdbcExceptionThrownWithTransactionRolledBack( e );
				return null;
			}
			else {
				throw getExceptionConverter().convert( e, makeLockOptions() );
			}
		}
		catch ( RuntimeException e ) {
			throw getExceptionConverter().convert( e, makeLockOptions() );
		}
	}

	protected EntityTransaction accessTransaction() {
		return loadAccessContext.getStatelessSession().accessTransaction();
	}

	protected ExceptionConverter getExceptionConverter() {
		return loadAccessContext.getStatelessSession().getExceptionConverter();
	}

	private T findByNaturalId(Object key) {
		final StatelessSessionImplementor session = loadAccessContext.getStatelessSession();

		performAnyNeededCrossReferenceSynchronizations(
				getNaturalIdSynchronization() != NaturalIdSynchronization.DISABLED,
				getEntityDescriptor(),
				session
		);

		final var normalizedKey = Helper.coerceNaturalId( getEntityDescriptor(), key );

		final Object cachedResolution = getCachedNaturalIdResolution( normalizedKey, session );
		if ( cachedResolution == INVALID_NATURAL_ID_REFERENCE ) {
			return null;
		}

		if ( cachedResolution != null ) {
			return findById( cachedResolution );
		}

		return withOptions( session, () -> (T) getEntityDescriptor()
				.getNaturalIdLoader()
				.load( normalizedKey, this, session ) );
	}

	private T withOptions(StatelessSessionImplementor session, Supplier<T> action) {
		var persistenceContext = session.getPersistenceContextInternal();

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

			if ( persistenceContext.isLoadFinished() ) {
				persistenceContext.clear();
			}
		}
	}

	private Object getCachedNaturalIdResolution(
			Object normalizedNaturalIdValue,
			StatelessSessionImplementor session) {
		return session
				.getPersistenceContextInternal()
				.getNaturalIdResolutions()
				.findCachedIdByNaturalId( normalizedNaturalIdValue, getEntityDescriptor() );
	}

	private T findById(Object key) {
		final Object keyToLoad = Helper.coerceId( getEntityDescriptor(), key, loadAccessContext.getStatelessSession().getFactory() );

		var session = loadAccessContext.getStatelessSession();

		var temporaryPersistenceContext = session.getPersistenceContext();
		if ( getEntityDescriptor().canReadFromCache() ) {
			final Object cachedEntity = loadFromSecondLevelCache( key, loadAccessContext );
			if ( cachedEntity != null ) {
				temporaryPersistenceContext.clear();
				//noinspection unchecked
				return (T) cachedEntity;
			}
		}

		return withOptions( session, ()-> {
			final Object result = getEntityDescriptor().load(
					keyToLoad,
					null,
					getNullSafeLockMode(),
					session
			);
			return (T) result;
		} );
	}

	private Object loadFromSecondLevelCache(
			Object key,
			StatelessLoadAccessContext context) {
		return CacheLoadHelper.loadFromSecondLevelCache(
				context.getStatelessSession(),
				null,
				getLockMode(),
				getEntityDescriptor(),
				new EntityKey( key, getEntityDescriptor() )
		);
	}

	private LockMode getNullSafeLockMode() {
		return getLockMode() == null ? LockMode.NONE : getLockMode();
	}
}
