/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.internal.CacheLoadHelper.PersistenceContextEntry;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.event.spi.LoadEventListener.GET;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.loader.internal.CacheLoadHelper.loadFromSessionCache;

/**
 * Base support for {@link MultiIdEntityLoader} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiIdEntityLoader<T> implements MultiIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	protected final EntityIdentifierMapping identifierMapping;

	public AbstractMultiIdEntityLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
		identifierMapping = getLoadable().getIdentifierMapping();
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	protected JdbcServices getJdbcServices() {
		return getSessionFactory().getJdbcServices();
	}

	protected SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory();
	}

	protected JdbcSelectExecutor getJdbcSelectExecutor() {
		return getJdbcServices().getJdbcSelectExecutor();
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	@Override
	public final <K> List<T> load(K[] ids, MultiIdLoadOptions loadOptions, EventSource session) {
		assert ids != null;
		return loadOptions.isOrderReturnEnabled()
				? performOrderedMultiLoad( ids, loadOptions, session )
				: performUnorderedMultiLoad( ids, loadOptions, session );
	}

	private List<T> performUnorderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		assert !loadOptions.isOrderReturnEnabled();
		assert ids != null;
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "#performUnorderedMultiLoad(`%s`, ..)", getLoadable().getEntityName() );
		}
		return unorderedMultiLoad( ids, loadOptions, lockOptions( loadOptions ), session );
	}

	protected List<T> performOrderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		assert loadOptions.isOrderReturnEnabled();
		assert ids != null;
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "#performOrderedMultiLoad(`%s`, ..)", getLoadable().getEntityName() );
		}
		return orderedMultiLoad( ids, loadOptions, lockOptions( loadOptions ), session );
	}

	private List<T> orderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session) {
		final boolean idCoercionEnabled = isIdCoercionEnabled();
		final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

		final int maxBatchSize = maxBatchSize( ids, loadOptions );

		final List<Object> result = arrayList( ids.length );

		final List<Object> idsInBatch = new ArrayList<>();
		final List<Integer> elementPositionsLoadedByBatch = new ArrayList<>();

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id = idCoercionEnabled ? idType.coerce( ids[i], session ) : ids[i];
			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );

			if ( !loadFromEnabledCaches( loadOptions, session, id, lockOptions, entityKey, result, i ) ) {
				// if we did not hit any of the continues above,
				// then we need to batch load the entity state.
				idsInBatch.add( id );

				if ( idsInBatch.size() >= maxBatchSize ) {
					// we've hit the allotted max-batch-size, perform an "intermediate load"
					loadEntitiesById( idsInBatch, lockOptions, loadOptions, session );
					idsInBatch.clear();
				}

				// Save the EntityKey instance for use later
				result.add( i, entityKey );
				elementPositionsLoadedByBatch.add( i );
			}
		}

		if ( !idsInBatch.isEmpty() ) {
			// we still have ids to load from the processing above since
			// the last max-batch-size trigger, perform a load for them
			loadEntitiesById( idsInBatch, lockOptions, loadOptions, session );
		}

		// for each result where we set the EntityKey earlier, replace them
		handleResults( loadOptions, session, elementPositionsLoadedByBatch, result );

		//noinspection unchecked
		return (List<T>) result;
	}

	protected static LockOptions lockOptions(MultiIdLoadOptions loadOptions) {
		return loadOptions.getLockOptions() == null
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();
	}

	protected abstract int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions);

	protected void handleResults(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			List<Integer> elementPositionsLoadedByBatch,
			List<Object> results) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		for ( Integer position : elementPositionsLoadedByBatch ) {
			// the element value at this position in the results List should be
			// the EntityKey for that entity - reuse it
			final EntityKey entityKey = (EntityKey) results.get( position );
			session.getPersistenceContextInternal().getBatchFetchQueue().removeBatchLoadableEntityKey( entityKey );
			final Object entity = persistenceContext.getEntity( entityKey );
			final Object result;
			if ( entity == null
				// the entity is locally deleted, and the options ask that we not return such entities
				|| !loadOptions.isReturnOfDeletedEntitiesEnabled()
					&& persistenceContext.getEntry( entity ).getStatus().isDeletedOrGone() ) {
				result = null;
			}
			else {
				result = persistenceContext.proxyFor( entity );
			}
			results.set( position, result );
		}
	}


	protected abstract void loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			EventSource session);

	protected boolean loadFromEnabledCaches(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			Object id,
			LockOptions lockOptions,
			EntityKey entityKey,
			List<Object> result,
			int i) {
		return ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() )
			&& isLoadFromCaches( loadOptions, entityKey, lockOptions, result, i, session );
	}

	private boolean isLoadFromCaches(
			MultiIdLoadOptions loadOptions,
			EntityKey entityKey,
			LockOptions lockOptions,
			List<Object> results, int i,
			EventSource session) {

		if ( loadOptions.isSessionCheckingEnabled() ) {
			// look for it in the Session first
			final PersistenceContextEntry entry =
					loadFromSessionCache( entityKey, lockOptions, GET, session );
			final Object entity = entry.entity();
			if ( entity != null ) {
				// put a null in the results
				final Object result =
						loadOptions.isReturnOfDeletedEntitiesEnabled()
							|| entry.isManaged()
								? entity : null;
				results.add( i, result );
				return true;
			}
		}

		if ( loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			// look for it in the second-level cache
			final Object entity =
					loadFromSecondLevelCache( entityKey, lockOptions, session );
			if ( entity != null ) {
				results.add( i, entity );
				return true;
			}
		}

		return false;
	}

	protected List<T> unorderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session) {
		final List<T> result = arrayList( ids.length );
		final Object[] unresolvableIds =
				resolveInCachesIfEnabled( ids, loadOptions, lockOptions, session,
						(position, entityKey, resolvedRef) -> result.add( (T) resolvedRef ) );
		if ( !isEmpty( unresolvableIds ) ) {
			loadEntitiesWithUnresolvedIds( loadOptions, lockOptions, session, unresolvableIds, result );
			final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();
			final EntityPersister persister = getLoadable().getEntityPersister();
			for ( Object id : unresolvableIds ) {
				// skip any of the null padded ids
				// (actually we could probably even break on the first null)
				if ( id != null ) {
					// found or not, remove the key from the batch-fetch queue
					batchFetchQueue.removeBatchLoadableEntityKey( session.generateEntityKey( id, persister ) );
				}
			}
		}
		return result;
	}

	protected abstract void loadEntitiesWithUnresolvedIds(
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session,
			Object[] unresolvableIds,
			List<T> results);

	protected final <R> Object[] resolveInCachesIfEnabled(
			Object[] ids,
			@NonNull MultiIdLoadOptions loadOptions,
			@NonNull LockOptions lockOptions,
			EventSource session,
			ResolutionConsumer<R> resolutionConsumer) {
		return loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled()
				// the user requested that we exclude ids corresponding to already managed
				// entities from the generated load SQL. So here we will iterate all
				// incoming id values and see whether it corresponds to an existing
				// entity associated with the PC. If it does, we add it to the results
				// list immediately and remove its id from the group of ids to load.
				// we'll load all of them from the database
				? resolveInCaches( ids, loadOptions, lockOptions, session, resolutionConsumer )
				: ids;
	}

	protected final <R> Object[] resolveInCaches(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session,
			ResolutionConsumer<R> resolutionConsumer) {

		final boolean idCoercionEnabled = isIdCoercionEnabled();
		final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

		List<Object> unresolvedIds = null;
		for ( int i = 0; i < ids.length; i++ ) {
			final Object id = idCoercionEnabled ? idType.coerce( ids[i], session ) : ids[i];
			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );
			unresolvedIds =
					loadFromCaches( loadOptions, lockOptions, resolutionConsumer, id, entityKey, unresolvedIds, i, session );
		}

		if ( isEmpty( unresolvedIds ) ) {
			// all the given ids were already associated with the Session
			return null;
		}
		else if ( unresolvedIds.size() == ids.length ) {
			// we need to load all the ids
			return ids;
		}
		else {
			// we need to load only some the ids
			return toIdArray( unresolvedIds );
		}
	}

	// Depending on the implementation, a specific subtype of Object[] (e.g. Integer[]) may be needed.
	protected abstract Object[] toIdArray(List<Object> ids);

	private boolean isIdCoercionEnabled() {
		return !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();
	}

	public interface ResolutionConsumer<T> {
		void consume(int position, EntityKey entityKey, T resolvedRef);
	}

	private <R> List<Object> loadFromCaches(
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			ResolutionConsumer<R> resolutionConsumer,
			Object id,
			EntityKey entityKey,
			List<Object> unresolvedIds, int i,
			EventSource session) {

		// look for it in the Session first
		final PersistenceContextEntry entry =
				loadFromSessionCache( entityKey, lockOptions, GET, session );
		final Object sessionEntity;
		if ( loadOptions.isSessionCheckingEnabled() ) {
			sessionEntity = entry.entity();
			if ( sessionEntity != null
					&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
					&& !entry.isManaged() ) {
				resolutionConsumer.consume( i, entityKey, null );
				return unresolvedIds;
			}
		}
		else {
			sessionEntity = null;
		}

		final Object cachedEntity =
				sessionEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled()
						? loadFromSecondLevelCache( entityKey, lockOptions, session )
						: sessionEntity;

		if ( cachedEntity != null ) {
			//noinspection unchecked
			resolutionConsumer.consume( i, entityKey, (R) cachedEntity );
		}
		else {
			if ( unresolvedIds == null ) {
				unresolvedIds = new ArrayList<>();
			}
			unresolvedIds.add( id );
		}
		return unresolvedIds;
	}

	private Object loadFromSecondLevelCache(EntityKey entityKey, LockOptions lockOptions, EventSource session) {
		final EntityPersister persister = getLoadable().getEntityPersister();
		return session.loadFromSecondLevelCache( persister, entityKey, null, lockOptions.getLockMode() );
	}
}
