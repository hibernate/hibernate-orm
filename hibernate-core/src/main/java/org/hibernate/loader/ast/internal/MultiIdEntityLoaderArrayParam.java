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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ManagedResultConsumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.type.descriptor.java.JavaType;

import static java.lang.Boolean.TRUE;
import static org.hibernate.engine.internal.BatchFetchQueueHelper.removeBatchLoadableEntityKey;
import static org.hibernate.engine.spi.SubselectFetch.createRegistrationHandler;
import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;
import static org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.loadFromSessionCacheStatic;
import static org.hibernate.loader.ast.internal.LoaderHelper.getReadOnlyFromLoadQueryInfluencers;
import static org.hibernate.loader.ast.internal.LoaderHelper.loadByArrayParameter;
import static org.hibernate.loader.ast.internal.LoaderSelectBuilder.createSelectBySingleArrayParameter;
import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.resolveArrayJdbcMapping;
import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/**
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderArrayParam<E> extends AbstractMultiIdEntityLoader<E> implements SqlArrayMultiKeyLoader {
	private final JdbcMapping arrayJdbcMapping;
	private final JdbcParameter jdbcParameter;

	public MultiIdEntityLoaderArrayParam(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		final Class<?> arrayClass = createTypedArray( 0 ).getClass();
		arrayJdbcMapping = resolveArrayJdbcMapping(
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
	protected void handleResults(
			MultiIdLoadOptions loadOptions,
			EventSource session,
			List<Integer> elementPositionsLoadedByBatch,
			List<Object> result) {
		final PersistenceContext persistenceContext = session.getPersistenceContext();
		for ( Integer position : elementPositionsLoadedByBatch ) {
			// the element value at this position in the result List should be
			// the EntityKey for that entity - reuse it
			final EntityKey entityKey = (EntityKey) result.get( position );
			removeBatchLoadableEntityKey( entityKey, session );
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

	@Override
	protected int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions) {
		final Integer explicitBatchSize = loadOptions.getBatchSize();
		return explicitBatchSize != null && explicitBatchSize > 0
				? explicitBatchSize
				// disable batching by default
				: ids.length;
	}

	@Override
	protected void loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			EventSource session) {
		final SelectStatement sqlAst = createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(1);
		jdbcParameterBindings.addBinding( jdbcParameter,
				new JdbcParameterBindingImpl( arrayJdbcMapping, idsInBatch.toArray( createTypedArray(0) ) ) );

		getJdbcSelectExecutor().executeQuery(
				getSqlAstTranslatorFactory().buildSelectTranslator( getSessionFactory(), sqlAst )
						.translate( NO_BINDINGS, QueryOptions.NONE ),
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler(
						session,
						createRegistrationHandler(
								session.getPersistenceContext().getBatchFetchQueue(),
								sqlAst,
								JdbcParametersList.singleton( jdbcParameter ),
								jdbcParameterBindings
						),
						TRUE.equals( loadOptions.getReadOnly( session ) ) ),
				RowTransformerStandardImpl.instance(),
				null,
				idsInBatch.size(),
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
		final LockOptions lockOptions = loadOptions.getLockOptions() == null
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

		final SelectStatement sqlAst = createSelectBySingleArrayParameter(
				getLoadable(),
				getIdentifierMapping(),
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParameter,
				getSessionFactory()
		);
		final JdbcOperationQuerySelect jdbcSelectOperation =
				getSqlAstTranslatorFactory().buildSelectTranslator( getSessionFactory(), sqlAst )
						.translate( NO_BINDINGS, QueryOptions.NONE );

		final List<E> databaseResults = loadByArrayParameter(
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
			removeBatchLoadableEntityKey( id, getLoadable(), session );
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

		boolean foundAnyResolvedEntities = false;
		List<K> nonResolvedIds = null;

		final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();
		final JavaType<?> idType = getLoadable().getIdentifierMapping().getJavaType();

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id = coerce ? idType.coerce( ids[i], session ) : ids[i];
			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );

			final LoadEvent loadEvent = new LoadEvent(
					id,
					getLoadable().getJavaType().getJavaTypeClass().getName(),
					lockOptions,
					session,
					getReadOnlyFromLoadQueryInfluencers( session )
			);

			Object managedEntity = null;

			// look for it in the Session first
			final PersistenceContextEntry persistenceContextEntry =
					loadFromSessionCacheStatic( loadEvent, entityKey, LoadEventListener.GET );
			if ( loadOptions.isSessionCheckingEnabled() ) {
				managedEntity = persistenceContextEntry.getEntity();

				if ( managedEntity != null
						&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
						&& !persistenceContextEntry.isManaged() ) {
					foundAnyResolvedEntities = true;
					resolutionConsumer.consume( i, entityKey, null );
					continue;
				}
			}

			if ( managedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				managedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
						loadEvent,
						getLoadable().getEntityPersister(),
						entityKey
				);
			}

			if ( managedEntity != null ) {
				foundAnyResolvedEntities = true;

				//noinspection unchecked
				resolutionConsumer.consume( i, entityKey, (R) managedEntity);
			}
			else {
				if ( nonResolvedIds == null ) {
					nonResolvedIds = new ArrayList<>();
				}
				//noinspection unchecked
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
