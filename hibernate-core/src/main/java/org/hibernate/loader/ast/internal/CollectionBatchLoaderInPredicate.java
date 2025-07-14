/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CollectionBatchLoader;
import org.hibernate.loader.ast.spi.SqlArrayMultiKeyLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.loader.ast.internal.MultiKeyLoadHelper.countIds;
import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;
import static org.hibernate.pretty.MessageHelper.collectionInfoString;

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
		sqlBatchSize = sessionFactory.getJdbcServices()
				.getDialect()
				.getBatchLoadSizingStrategy()
				.determineOptimalBatchLoadSize( keyColumnCount, domainBatchSize, false );
		if ( MULTI_KEY_LOAD_LOGGER.isTraceEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.tracef(
					"Batch fetching enabled for collection '%s' using IN-predicate with batch size %s (%s)",
					attributeMapping.getNavigableRole().getFullPath(),
					sqlBatchSize,
					domainBatchSize
			);
		}

		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				sqlBatchSize,
				influencers,
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);

		final QuerySpec querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final TableGroup tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		this.jdbcParameters = jdbcParametersBuilder.build();
		assert this.jdbcParameters.size() == this.sqlBatchSize * this.keyColumnCount;

		jdbcSelect = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	void initializeKeys(Object key, Object[] keysToInitialize, SharedSessionContractImplementor session) {
		final boolean loggerDebugEnabled = MULTI_KEY_LOAD_LOGGER.isDebugEnabled();
		if ( loggerDebugEnabled ) {
			MULTI_KEY_LOAD_LOGGER.tracef(
					"Collection keys to initialize via batch fetching (%s) %s",
					collectionInfoString( getLoadable(), key ),
					keysToInitialize
			);
		}

		final MultiKeyLoadChunker<Object> chunker = new MultiKeyLoadChunker<>(
				sqlBatchSize,
				keyColumnCount,
				getLoadable().getKeyDescriptor(),
				jdbcParameters,
				sqlAst,
				jdbcSelect
		);

		final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();

		chunker.processChunks(
				keysToInitialize,
				countIds( keysToInitialize ),
				(jdbcParameterBindings, session1) -> {
					// Create a RegistrationHandler for handling any subselect fetches we encounter handling this chunk
					final SubselectFetch.RegistrationHandler registrationHandler = SubselectFetch.createRegistrationHandler(
							batchFetchQueue,
							sqlAst,
							jdbcParameters,
							jdbcParameterBindings
					);
					return new ExecutionContextWithSubselectFetchHandler( session, registrationHandler );
				},
				(key1, relativePosition, absolutePosition) -> {
				},
				(startIndex) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.tracef(
								"Processing collection batch-fetch chunk (%s) %s - %s",
								collectionInfoString( getLoadable(), key ),
								startIndex,
								startIndex + (sqlBatchSize-1)
						);
					}
				},
				(startIndex, nonNullElementCount) -> {
					if ( loggerDebugEnabled ) {
						MULTI_KEY_LOAD_LOGGER.tracef(
								"Finishing collection batch-fetch chunk (%s) %s - %s (%s)",
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
