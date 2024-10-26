/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.loadFromSessionCacheStatic;
import static org.hibernate.loader.ast.internal.LoaderHelper.getReadOnlyFromLoadQueryInfluencers;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * Base support for {@link MultiIdEntityLoader} implementations.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiIdEntityLoader<T> implements MultiIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;
	private final EntityIdentifierMapping identifierMapping;

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
		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( ids, loadOptions, session );
		}
		else {
			return performUnorderedMultiLoad( ids, loadOptions, session );
		}
	}

	protected List<T> performOrderedMultiLoad(
			Object[] ids,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "#performOrderedMultiLoad(`%s`, ..)", getLoadable().getEntityName() );
		}

		assert loadOptions.isOrderReturnEnabled();

		final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();
		final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

		final LockOptions lockOptions = loadOptions.getLockOptions() == null
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final int maxBatchSize = maxBatchSize( ids, loadOptions );

		final List<Object> result = arrayList( ids.length );

		final List<Object> idsInBatch = new ArrayList<>();
		final List<Integer> elementPositionsLoadedByBatch = new ArrayList<>();

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id = coerce ? idType.coerce( ids[i], session ) : ids[i];
			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );

			if ( !loadFromCaches( loadOptions, session, id, lockOptions, entityKey, result, i ) ) {
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

	protected abstract int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions);

	protected abstract void handleResults(MultiIdLoadOptions loadOptions, EventSource session, List<Integer> elementPositionsLoadedByBatch, List<Object> result);

	protected abstract void loadEntitiesById(List<Object> idsInBatch, LockOptions lockOptions, MultiIdLoadOptions loadOptions, EventSource session);

	protected boolean loadFromCaches(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			Object id,
			LockOptions lockOptions,
			EntityKey entityKey,
			List<Object> result,
			int i) {
		if ( loadOptions.isSessionCheckingEnabled() || loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			final LoadEvent loadEvent = new LoadEvent(
					id,
					getLoadable().getJavaType().getJavaTypeClass().getName(),
					lockOptions,
					session,
					getReadOnlyFromLoadQueryInfluencers( session )
			);

			Object managedEntity = null;

			if ( loadOptions.isSessionCheckingEnabled() ) {
				// look for it in the Session first
				final CacheEntityLoaderHelper.PersistenceContextEntry persistenceContextEntry =
						loadFromSessionCacheStatic( loadEvent, entityKey, LoadEventListener.GET );
				managedEntity = persistenceContextEntry.getEntity();

				if ( managedEntity != null
						&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
						&& !persistenceContextEntry.isManaged() ) {
					// put a null in the result
					result.add( i, null );
					return true;
				}
			}

			if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				// look for it in the SessionFactory
				managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
						loadEvent,
						getLoadable().getEntityPersister(),
						entityKey
				);
			}

			if ( managedEntity != null ) {
				result.add( i, managedEntity );
				return true;
			}
		}
		return false;
	}

	protected abstract <K> List<T> performUnorderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session);

}
