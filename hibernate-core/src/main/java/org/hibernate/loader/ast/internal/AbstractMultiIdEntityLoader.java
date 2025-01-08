/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
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

	protected abstract void handleResults(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			List<Integer> elementPositionsLoadedByBatch,
			List<Object> result);

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
		if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			return isLoadFromCaches( loadOptions, entityKey, lockOptions, result, i, session );
		}
		else {
			return false;
		}
	}

	private boolean isLoadFromCaches(
			MultiIdLoadOptions loadOptions,
			EntityKey entityKey,
			LockOptions lockOptions,
			List<Object> result, int i,
			EventSource session) {
		Object managedEntity = null;

		if ( loadOptions.isSessionCheckingEnabled() ) {
			// look for it in the Session first
			final PersistenceContextEntry persistenceContextEntry =
					loadFromSessionCache( entityKey, lockOptions, GET, session );
			managedEntity = persistenceContextEntry.entity();

			if ( managedEntity != null
					&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
					&& !persistenceContextEntry.isManaged() ) {
				// put a null in the result
				result.add( i, null );
				return true;
			}
		}

		if ( managedEntity == null
				&& loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			// look for it in the SessionFactory
			final EntityPersister persister = getLoadable().getEntityPersister();
			managedEntity = session.loadFromSecondLevelCache( persister, entityKey, null, lockOptions.getLockMode() );
		}

		if ( managedEntity != null ) {
			result.add( i, managedEntity );
			return true;
		}
		else {
			return false;
		}
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
		}
		return result;
	}

	protected abstract void loadEntitiesWithUnresolvedIds(
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session,
			Object[] unresolvableIds,
			List<T> result);

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
				// entity associated with the PC. If it does, we add it to the result
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
		Object cachedEntity = null;

		// look for it in the Session first
		final PersistenceContextEntry persistenceContextEntry =
				loadFromSessionCache( entityKey, lockOptions, GET, session );
		if ( loadOptions.isSessionCheckingEnabled() ) {
			cachedEntity = persistenceContextEntry.entity();
			if ( cachedEntity != null
					&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
					&& !persistenceContextEntry.isManaged() ) {
				resolutionConsumer.consume( i, entityKey, null );
				return unresolvedIds;
			}
		}

		if ( cachedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			final EntityPersister persister = getLoadable().getEntityPersister();
			cachedEntity = session.loadFromSecondLevelCache( persister, entityKey, null, lockOptions.getLockMode() );
		}

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

}
