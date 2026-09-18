/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import java.util.List;

import jakarta.annotation.Nonnull;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
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

/**
 * Standard implementation of {@link org.hibernate.loader.ast.spi.MultiIdEntityLoader}
 * which uses a SQL {@code in} condition containing multiple JDBC parameters.
 *
 * @author Steve Ebersole
 */
public class MultiIdEntityLoaderInPredicate<T> extends AbstractMultiIdEntityLoader<T> {

	private final int idJdbcTypeCount;

	public MultiIdEntityLoaderInPredicate(
			@Nonnull EntityPersister entityDescriptor,
			int idColumnSpan,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		super( entityDescriptor, sessionFactory );
		idJdbcTypeCount = idColumnSpan;
		assert idJdbcTypeCount > 0;
	}

	private boolean isInClauseParameterPaddingEnabled() {
		return getSessionFactory().getSessionFactoryOptions().inClauseParameterPaddingEnabled();
	}

	@Nonnull
	private MultiKeyLoadSizingStrategy getBatchLoadSizingStrategy() {
		return getJdbcServices().getJdbcEnvironment().getDialect().getBatchLoadSizingStrategy();
	}

	@Override
	protected int maxBatchSize(@Nonnull Object[] ids, @Nonnull MultiIdLoadOptions loadOptions) {
		final Integer explicitBatchSize = loadOptions.getBatchSize();
		return explicitBatchSize != null && explicitBatchSize > 0
				? explicitBatchSize
				: getBatchLoadSizingStrategy()
						.determineOptimalBatchLoadSize( idJdbcTypeCount, ids.length,
								isInClauseParameterPaddingEnabled() );
	}

	@Override
	protected void loadEntitiesById(
			@Nonnull List<Object> idsInBatch,
			@Nonnull LockOptions lockOptions,
			@Nonnull MultiIdLoadOptions loadOptions,
			@Nonnull SharedSessionContractImplementor session) {
		assert idsInBatch != null;
		assert !idsInBatch.isEmpty();
		listEntitiesById( idsInBatch, lockOptions, loadOptions, session );
	}

	@Nonnull
	private List<T> listEntitiesById(
			@Nonnull List<Object> idsInBatch,
			@Nonnull LockOptions lockOptions,
			@Nonnull MultiIdLoadOptions loadOptions,
			@Nonnull SharedSessionContractImplementor session) {
		final int numberOfIdsInBatch = idsInBatch.size();
		return numberOfIdsInBatch == 1
				? performSingleMultiLoad( idsInBatch.get( 0 ), lockOptions, session )
				: performRegularMultiLoad( idsInBatch, lockOptions, loadOptions, session, numberOfIdsInBatch );
	}

	@Nonnull
	private List<T> performRegularMultiLoad(
			@Nonnull List<Object> idsInBatch,
			@Nonnull LockOptions lockOptions,
			@Nonnull MultiIdLoadOptions loadOptions,
			@Nonnull SharedSessionContractImplementor session,
			int numberOfIdsInBatch) {
//		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
//			MULTI_KEY_LOAD_LOGGER.tracef( "#loadEntitiesById(`%s`, `%s`, ..)",
//					getLoadable().getEntityName(), numberOfIdsInBatch );
//		}

		final var jdbcParametersBuilder =
				JdbcParametersList.newBuilder( numberOfIdsInBatch * idJdbcTypeCount );

		final var sqlAst =
				createSelect(
						getLoadable(),
						// null here means to select everything
						null,
						getLoadable().getIdentifierMapping(),
						null,
						numberOfIdsInBatch,
						session.getLoadQueryInfluencers(),
						lockOptions,
						jdbcParametersBuilder::add,
						new SqlAliasBaseManager(),
						getSessionFactory()
				);

		final var jdbcParameters = jdbcParametersBuilder.build();
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
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
				getSqlAstTranslatorFactory().buildTranslator( new SqlAstTranslationRequest.Select( getSessionFactory(), sqlAst ) )
						.translate( jdbcParameterBindings, new QueryOptionsAdapter() {
							@Override
							@Nonnull
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

	@Nullable
	private SubselectFetch.RegistrationHandler fetchableKeysHandler(
			@Nonnull SharedSessionContractImplementor session,
			@Nonnull SelectStatement sqlAst,
			@Nonnull JdbcParametersList jdbcParameters,
			@Nonnull JdbcParameterBindings jdbcParameterBindings) {
		final var batchFetchQueue = session.getPersistenceContext().getBatchFetchQueue();
		return session.getLoadQueryInfluencers().hasSubselectLoadableAttributes( getLoadable().getEntityPersister() )
				? createRegistrationHandler( batchFetchQueue, sqlAst, jdbcParameters, jdbcParameterBindings )
				: null;
	}

	@Nonnull
	private List<T> performSingleMultiLoad(@Nonnull Object id, @Nonnull LockOptions lockOptions, @Nonnull SharedSessionContractImplementor session) {
		final Object entity = getLoadable().getEntityPersister().load( id, null, lockOptions, session );
		@SuppressWarnings("unchecked") T loaded = (T) entity;
		return singletonList( loaded );
	}

	@Override
	protected void loadEntitiesWithUnresolvedIds(
			@Nonnull Object[] unresolvableIds,
			@Nonnull MultiIdLoadOptions loadOptions,
			@Nonnull LockOptions lockOptions,
			@Nonnull List<T> results,
			@Nonnull SharedSessionContractImplementor session) {
		final int maxBatchSize = maxBatchSize( unresolvableIds, loadOptions );
		int numberOfIdsLeft = unresolvableIds.length;
		int idPosition = 0;
		while ( numberOfIdsLeft > 0 ) {
			final int batchSize =  Math.min( numberOfIdsLeft, maxBatchSize );
			final Object[] idsInBatch = new Object[batchSize];
			arraycopy( unresolvableIds, idPosition, idsInBatch, 0, batchSize );
			if ( idsInBatch.length == 1 ) {
				final var singleResult = getLoadable().getEntityPersister()
						.load( idsInBatch[0], null, lockOptions, session );
				if ( singleResult != null ) {
					//noinspection unchecked
					results.add( (T) singleResult );
				}
			}
			else {
				results.addAll( performRegularMultiLoad( asList( idsInBatch ), lockOptions, loadOptions, session, idsInBatch.length ) );
			}
			numberOfIdsLeft = numberOfIdsLeft - batchSize;
			idPosition += batchSize;
		}
	}

	@Nonnull
	@Override
	protected Object[] toIdArray(@Nonnull List<Object> ids) {
		// This loader implementation doesn't need arrays to have a specific type, Object[] will do.
		return ids.toArray();
	}
}
