/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.internal.BatchFetchQueueHelper;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.PersistenceContextEntry;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ManagedResultConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.type.descriptor.java.JavaType;

import static java.lang.Boolean.TRUE;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.loadFromSessionCacheStatic;

/**
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderArrayParam<E> extends AbstractMultiIdEntityLoader<E> implements SqlArrayMultiKeyLoader {
	private final JdbcMapping arrayJdbcMapping;
	private final JdbcParameter jdbcParameter;
	private final int idJdbcTypeCount;

	public MultiIdEntityLoaderArrayParam(EntityMappingType entityDescriptor, int identifierColumnSpan, SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		this.idJdbcTypeCount = identifierColumnSpan;
		final Class<?> arrayClass = createTypedArray( 0 ).getClass();
		arrayJdbcMapping = MultiKeyLoadHelper.resolveArrayJdbcMapping(
				getSessionFactory().getTypeConfiguration().getBasicTypeRegistry().getRegisteredType( arrayClass ),
				getIdentifierMapping().getJdbcMapping(),
				arrayClass,
				getSessionFactory()
		);
		jdbcParameter = new JdbcParameterImpl( arrayJdbcMapping );
	}

	@Override
	public BasicEntityIdentifierMapping getIdentifierMapping() {
		return (BasicEntityIdentifierMapping) super.getIdentifierMapping();
	}

	@Override
	protected <K> List<E> performOrderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef(
					"MultiIdEntityLoaderArrayParam#performOrderedMultiLoad - %s",
					getLoadable().getEntityName()
			);
		}

		assert loadOptions.isOrderReturnEnabled();

		final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();
		final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

		final LockOptions lockOptions = loadOptions.getLockOptions() == null
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		final int maxBatchSize = maxBatchSize( ids, loadOptions );

		final List<Object> result = CollectionHelper.arrayList( ids.length );

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
					loadEntitiesById( loadOptions, session, lockOptions, idsInBatch );
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
			loadEntitiesById( loadOptions, session, lockOptions, idsInBatch );
		}

		// for each result where we set the EntityKey earlier, replace them
		handleResults( loadOptions, session, elementPositionsLoadedByBatch, result );

		//noinspection unchecked
		return (List<E>) result;
	}

	private boolean loadFromCaches(
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
					LoaderHelper.getReadOnlyFromLoadQueryInfluencers( session )
			);

			Object managedEntity = null;

			if ( loadOptions.isSessionCheckingEnabled() ) {
				// look for it in the Session first
				final PersistenceContextEntry persistenceContextEntry =
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

	private static void handleResults(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			List<Integer> elementPositionsLoadedByBatch,
			List<Object> result) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		for ( Integer position : elementPositionsLoadedByBatch ) {
			// the element value at this position in the result List should be
			// the EntityKey for that entity - reuse it
			final EntityKey entityKey = (EntityKey) result.get( position );
			BatchFetchQueueHelper.removeBatchLoadableEntityKey( entityKey, session );
			Object entity = persistenceContext.getEntity( entityKey );
			if ( entity != null && !loadOptions.isReturnOfDeletedEntitiesEnabled() ) {
				// make sure it is not DELETED
				final EntityEntry entry = persistenceContext.getEntry( entity );
				if ( entry.getStatus().isDeletedOrGone() ) {
					// the entity is locally deleted, and the options ask that we not return such entities...
					entity = null;
				}
				else {
					entity = persistenceContext.proxyFor( entity );
				}
			}
			result.set( position, entity );
		}
	}

	private <K> int maxBatchSize(K[] ids, MultiIdLoadOptions loadOptions) {
		if ( loadOptions.getBatchSize() != null && loadOptions.getBatchSize() > 0 ) {
			return loadOptions.getBatchSize();
		}
		else {
			// disable batching by default
			return ids.length;
//			return getSessionFactory().getJdbcServices().getJdbcEnvironment().getDialect()
//					.getBatchLoadSizingStrategy().determineOptimalBatchLoadSize(
//							idJdbcTypeCount,
//							ids.length,
//							getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled()
//					);
		}
	}

	private void loadEntitiesById(
			MultiIdLoadOptions loadOptions, EventSource session, LockOptions lockOptions, List<Object> idsToLoadFromDatabase) {
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);
		final JdbcOperationQuerySelect jdbcSelectOperation = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(1);
		jdbcParameterBindings.addBinding(
				jdbcParameter,
				new JdbcParameterBindingImpl( arrayJdbcMapping, idsToLoadFromDatabase.toArray( createTypedArray(0 ) ) )
		);

		final PersistenceContext persistenceContext = session.getPersistenceContext();
		final BatchFetchQueue batchFetchQueue = persistenceContext.getBatchFetchQueue();

		final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				JdbcParametersList.singleton( jdbcParameter ),
				jdbcParameterBindings
		);

		session.getJdbcServices().getJdbcSelectExecutor().executeQuery(
				jdbcSelectOperation,
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler( session,
						subSelectFetchableKeysHandler,
						TRUE.equals( loadOptions.getReadOnly( session ) ) ),
				RowTransformerStandardImpl.instance(),
				null,
				idsToLoadFromDatabase.size(),
				ManagedResultConsumer.INSTANCE
		);
	}


	@Override
	protected <K> List<E> performUnorderedMultiLoad(
			K[] ids,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		if ( MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER.tracef(
					"MultiIdEntityLoaderArrayParam#performUnorderedMultiLoad - %s",
					getLoadable().getEntityName()
			);
		}

		final List<E> result = CollectionHelper.arrayList( ids.length );
		final LockOptions lockOptions = (loadOptions.getLockOptions() == null)
				? new LockOptions( LockMode.NONE )
				: loadOptions.getLockOptions();

		//noinspection unchecked
		final K[] idsToLoadFromDatabase = processResolvableEntities(
				ids,
				(index, entityKey, resolvedEntity) -> result.add( (E) resolvedEntity ),
				loadOptions,
				lockOptions,
				session
		);

		if ( idsToLoadFromDatabase == null ) {
			// all the given ids were already associated with the Session
			return result;
		}

		final SelectStatement sqlAst = LoaderSelectBuilder.createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);
		final JdbcOperationQuerySelect jdbcSelectOperation = getSessionFactory().getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( getSessionFactory(), sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );

		final List<E> databaseResults = LoaderHelper.loadByArrayParameter(
				idsToLoadFromDatabase,
				sqlAst,
				jdbcSelectOperation,
				jdbcParameter,
				arrayJdbcMapping,
				null,
				null,
				null,
				lockOptions,
				session.isDefaultReadOnly(),
				session
		);
		result.addAll( databaseResults );

		//noinspection ForLoopReplaceableByForEach
		for ( int i = 0; i < idsToLoadFromDatabase.length; i++ ) {
			final Object id = idsToLoadFromDatabase[i];
			if ( id == null ) {
				// skip any of the null padded ids
				//		- actually we could probably even break here
				continue;
			}
			// found or not, remove the key from the batch-fetch queue
			BatchFetchQueueHelper.removeBatchLoadableEntityKey( id, getLoadable(), session );
		}

		return result;
	}
	public interface ResolutionConsumer<T> {
		void consume(int position, EntityKey entityKey, T resolvedRef);
	}

	protected final <R,K> K[] processResolvableEntities(
			K[] ids,
			ResolutionConsumer<R> resolutionConsumer,
			@NonNull MultiIdLoadOptions loadOptions,
			@NonNull LockOptions lockOptions,
			EventSource session) {
		if ( !loadOptions.isSessionCheckingEnabled()
				&& !loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			// we'll load all of them from the database
			return ids;
		}

		final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();

		boolean foundAnyResolvedEntities = false;
		List<K> nonResolvedIds = null;

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id;
			if ( coerce ) {
				//noinspection unchecked
				id = (K) getLoadable().getIdentifierMapping().getJavaType().coerce( ids[i], session );
			}
			else {
				id = ids[i];
			}

			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );
			final LoadEvent loadEvent = new LoadEvent(
					id,
					getLoadable().getJavaType().getJavaTypeClass().getName(),
					lockOptions,
					session,
					LoaderHelper.getReadOnlyFromLoadQueryInfluencers( session )
			);

			Object resolvedEntity = null;

			// look for it in the Session first
			final PersistenceContextEntry persistenceContextEntry = loadFromSessionCacheStatic(
					loadEvent,
					entityKey,
					LoadEventListener.GET
			);
			if ( loadOptions.isSessionCheckingEnabled() ) {
				resolvedEntity = persistenceContextEntry.getEntity();

				if ( resolvedEntity != null
						&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
						&& !persistenceContextEntry.isManaged() ) {
					foundAnyResolvedEntities = true;
					resolutionConsumer.consume( i, entityKey, null );
					continue;
				}
			}

			if ( resolvedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				resolvedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
						loadEvent,
						getLoadable().getEntityPersister(),
						entityKey
				);
			}

			if ( resolvedEntity != null ) {
				foundAnyResolvedEntities = true;

				//noinspection unchecked
				resolutionConsumer.consume( i, entityKey, (R) resolvedEntity);
			}
			else {
				if ( nonResolvedIds == null ) {
					nonResolvedIds = new ArrayList<>();
				}
				//noinspection unchecked,CastCanBeRemovedNarrowingVariableType
				nonResolvedIds.add( (K) id );
			}
		}

		if ( foundAnyResolvedEntities ) {
			if ( isEmpty( nonResolvedIds ) ) {
				// all the given ids were already associated with the Session
				return null;
			}

			return nonResolvedIds.toArray( createTypedArray(0) );
		}

		return ids;
	}

	private <X> X[] createTypedArray(@SuppressWarnings("SameParameterValue") int length) {
		//noinspection unchecked
		return (X[]) Array.newInstance( getIdentifierMapping().getJavaType().getJavaTypeClass(), length );
	}
}
