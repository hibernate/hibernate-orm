/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.lang.Boolean.TRUE;
import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hibernate.engine.spi.SubselectFetch.createRegistrationHandler;
import static org.hibernate.loader.ast.internal.LoaderSelectBuilder.createSelect;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * Standard implementation of {@link org.hibernate.loader.ast.spi.MultiIdEntityLoader}
 * which uses a SQL {@code in} condition containing multiple JDBC parameters.
 *
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderInPredicate<T> extends AbstractMultiIdEntityLoader<T> {

	private final int idJdbcTypeCount;

	public MultiIdEntityLoaderInPredicate(
			EntityPersister entityDescriptor,
			int idColumnSpan,
			SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		idJdbcTypeCount = idColumnSpan;
		assert idJdbcTypeCount > 0;
	}

	private boolean isInClauseParameterPaddingEnabled() {
		return getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled();
	}

	private MultiKeyLoadSizingStrategy getBatchLoadSizingStrategy() {
		return getJdbcServices().getJdbcEnvironment().getDialect().getBatchLoadSizingStrategy();
	}

	@Override
	protected int maxBatchSize(Object[] ids, MultiIdLoadOptions loadOptions) {
		final Integer explicitBatchSize = loadOptions.getBatchSize();
		return explicitBatchSize != null && explicitBatchSize > 0
				? explicitBatchSize
				: getBatchLoadSizingStrategy()
						.determineOptimalBatchLoadSize( idJdbcTypeCount, ids.length,
								isInClauseParameterPaddingEnabled() );
	}

	@Override
	protected void loadEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		assert idsInBatch != null;
		assert !idsInBatch.isEmpty();
		listEntitiesById( idsInBatch, lockOptions, loadOptions, session );
	}

	private List<T> listEntitiesById(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			SharedSessionContractImplementor session) {
		final int numberOfIdsInBatch = idsInBatch.size();
		return numberOfIdsInBatch == 1
				? performSingleMultiLoad( idsInBatch.get( 0 ), lockOptions, session )
				: performRegularMultiLoad( idsInBatch, lockOptions, loadOptions, session, numberOfIdsInBatch );
	}

	private List<T> performRegularMultiLoad(
			List<Object> idsInBatch,
			LockOptions lockOptions,
			MultiIdLoadOptions loadOptions,
			SharedSessionContractImplementor session,
			int numberOfIdsInBatch) {
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef( "#loadEntitiesById(`%s`, `%s`, ..)",
					getLoadable().getEntityName(), numberOfIdsInBatch );
		}

		final JdbcParametersList.Builder jdbcParametersBuilder =
				JdbcParametersList.newBuilder( numberOfIdsInBatch * idJdbcTypeCount );

		final SelectStatement sqlAst = createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				getLoadable().getIdentifierMapping(),
				null,
				numberOfIdsInBatch,
				session.getLoadQueryInfluencers(),
				lockOptions,
				jdbcParametersBuilder::add,
				getSessionFactory()
		);

		final JdbcParametersList jdbcParameters = jdbcParametersBuilder.build();
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = 0;
		for ( int i = 0; i < numberOfIdsInBatch; i++ ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					idsInBatch.get( i ),
					offset,
					getLoadable().getIdentifierMapping(),
					jdbcParameters,
					session
			);
		}
		// we should have used all the JdbcParameter references (created bindings for all)
		assert offset == jdbcParameters.size();

		return getJdbcSelectExecutor().list(
				getSqlAstTranslatorFactory().buildSelectTranslator( getSessionFactory(), sqlAst )
						.translate( jdbcParameterBindings, new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						} ),
				jdbcParameterBindings,
				new ExecutionContextWithSubselectFetchHandler(
						session,
						fetchableKeysHandler( session, sqlAst, jdbcParameters, jdbcParameterBindings ),
						session instanceof SessionImplementor statefulSession
								&& TRUE.equals( loadOptions.getReadOnly( statefulSession ) ),
						lockOptions
				),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				idsInBatch.size()
		);
	}

	private SubselectFetch.RegistrationHandler fetchableKeysHandler(
			SharedSessionContractImplementor session,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings) {
		final BatchFetchQueue batchFetchQueue = session.getPersistenceContext().getBatchFetchQueue();
		return session.getLoadQueryInfluencers().hasSubselectLoadableCollections( getLoadable().getEntityPersister() )
				? createRegistrationHandler( batchFetchQueue, sqlAst, jdbcParameters, jdbcParameterBindings )
				: null;
	}

	private List<T> performSingleMultiLoad(Object id, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final Object entity = getLoadable().getEntityPersister().load( id, null, lockOptions, session );
		@SuppressWarnings("unchecked") T loaded = (T) entity;
		return singletonList( loaded );
	}

	@Override
	protected void loadEntitiesWithUnresolvedIds(
			Object[] unresolvableIds,
			MultiIdLoadOptions loadOptions,
			LockOptions lockOptions,
			List<T> results,
			SharedSessionContractImplementor session) {
		final int maxBatchSize = maxBatchSize( unresolvableIds, loadOptions );
		int numberOfIdsLeft = unresolvableIds.length;
		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			final int batchSize =  Math.min( numberOfIdsLeft, maxBatchSize );
			final Object[] idsInBatch = new Object[batchSize];
			arraycopy( unresolvableIds, idPosition, idsInBatch, 0, batchSize );
			results.addAll( listEntitiesById( asList( idsInBatch ), lockOptions, loadOptions, session ) );
			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}
	}

	@Override
	protected Object[] toIdArray(List<Object> ids) {
		// This loader implementation doesn't need arrays to have a specific type, Object[] will do.
		return ids.toArray();
	}
}
