/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.countIds;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;
import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/**
 * {@link CollectionBatchLoader} for batch fetching using a SQL {@code IN} predicate.
 *
 * @author Steve Ebersole
 */
public class CollectionBatchLoaderInPredicate
		extends AbstractCollectionBatchLoader
		implements SqlArrayMultiKeyLoader {
	private final int keyColumnCount;
	private final int sqlBatchSize;
	private final JdbcParametersList jdbcParameters;
	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelect;

	public CollectionBatchLoaderInPredicate(
			int domainBatchSize,
			LoadQueryInfluencers influencers,
			PluralAttributeMapping attributeMapping,
			SessionFactoryImplementor sessionFactory) {
		super( domainBatchSize, influencers, attributeMapping, sessionFactory );

		keyColumnCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();
		sqlBatchSize =
				sessionFactory.getJdbcServices().getDialect()
						.getBatchLoadSizingStrategy()
						.determineOptimalBatchLoadSize( keyColumnCount, domainBatchSize, false );
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.enabledCollectionInPredicate(
					attributeMapping.getNavigableRole().getFullPath(),
					sqlBatchSize,
					domainBatchSize
			);
		}

		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				sqlBatchSize,
				influencers,
				new LockOptions(),
				jdbcParametersBuilder::add,
				sessionFactory
		);

		final var querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final var tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		jdbcParameters = jdbcParametersBuilder.build();
		assert jdbcParameters.size() == sqlBatchSize * keyColumnCount;

		jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqlAst )
						.translate( NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	void initializeKeys(Object key, Object[] keysToInitialize, SharedSessionContractImplementor session) {
		final boolean loggerDebugEnabled = MULTI_KEY_LOAD_LOGGER.isDebugEnabled();
		if ( loggerDebugEnabled ) {
			MULTI_KEY_LOAD_LOGGER.collectionKeysToInitialize(
					collectionInfoString( getLoadable(), key ),
					keysToInitialize
			);
		}

		final var chunker = new MultiKeyLoadChunker<>(
				sqlBatchSize,
				keyColumnCount,
				getLoadable().getKeyDescriptor(),
				jdbcParameters,
				sqlAst,
				jdbcSelect
		);

		final var batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();

		chunker.processChunks(
				keysToInitialize,
				countIds( keysToInitialize ),
				(jdbcParameterBindings, session1) ->
						// Create a RegistrationHandler for handling any
						// subselect fetches we encounter handling this chunk
						new ExecutionContextWithSubselectFetchHandler(
								session,
								SubselectFetch.createRegistrationHandler(
										batchFetchQueue,
										sqlAst,
										jdbcParameters,
										jdbcParameterBindings
								)
						),
				(key1, relativePosition, absolutePosition) -> {
				},
				(startIndex) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.processingCollectionBatchFetchChunk(
								collectionInfoString( getLoadable(), key ),
								startIndex,
								startIndex + (sqlBatchSize-1)
						);
					}
				},
				(startIndex, nonNullElementCount) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.finishingCollectionBatchFetchChunk(
								collectionInfoString( getLoadable(), key ),
								startIndex,
								startIndex + (sqlBatchSize-1),
								nonNullElementCount
						);
					}
					for ( int i = 0; i < nonNullElementCount; i++ ) {
						final int keyPosition = i + startIndex;
						if ( keyPosition < keysToInitialize.length ) {
							finishInitializingKey( keysToInitialize[keyPosition], session );
						}
					}
				},
				session
		);
	}

	@Override
	void finishInitializingKeys(Object[] key, SharedSessionContractImplementor session) {
		// do nothing
	}
}
