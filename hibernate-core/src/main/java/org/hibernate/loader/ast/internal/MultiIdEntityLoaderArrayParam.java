/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
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

import static java.lang.Boolean.TRUE;
import static org.hibernate.engine.internal.BatchFetchQueueHelper.removeBatchLoadableEntityKey;
import static org.hibernate.engine.spi.SubselectFetch.createRegistrationHandler;
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
		arrayJdbcMapping = resolveArrayJdbcMapping(
				getIdentifierMapping().getJdbcMapping(),
				identifierMapping.getJavaType().getJavaTypeClass(),
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
				new JdbcParameterBindingImpl( arrayJdbcMapping, idsInBatch ) );

		getJdbcSelectExecutor().executeQuery(
				getSqlAstTranslatorFactory().buildSelectTranslator( getSessionFactory(), sqlAst )
						.translate( NO_BINDINGS, new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						} ),
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
	protected void loadEntitiesWithUnresolvedIds(
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			EventSource session,
			Object[] unresolvableIds,
			List<E> result) {
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
				unresolvableIds,
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

		for ( Object id : unresolvableIds ) {
			// skip any of the null padded ids
			// (actually we could probably even break on the first null)
			if ( id != null ) {
				// found or not, remove the key from the batch-fetch queue
				removeBatchLoadableEntityKey( id, getLoadable(), session );
			}
		}
	}

}
