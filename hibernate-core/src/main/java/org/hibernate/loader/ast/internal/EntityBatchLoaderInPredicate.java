/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.Arrays;
import java.util.Locale;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.EntityBatchLoader;
import org.hibernate.loader.ast.spi.SqlInPredicateMultiKeyLoader;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.loader.ast.internal.MultiKeyLoadLogging.MULTI_KEY_LOAD_LOGGER;

/**
 * An {@link EntityBatchLoader} using one or more SQL queries, which each initialize up
 * to {@linkplain #getSqlBatchSize()} entities using a SQL IN predicate restriction -
 * e.g., {@code ... where id in (?,?,...)}.
 * <p>
 * The number of parameters rendered into the SQL is controlled by {@linkplain #getSqlBatchSize()}.
 * Any unused parameter slots for a particular execution are set to {@code null}.
 *
 * @author Steve Ebersole
 */
public class EntityBatchLoaderInPredicate<T>
		extends AbstractEntityBatchLoader<T>
		implements SqlInPredicateMultiKeyLoader {
	private final int domainBatchSize;
	private final int sqlBatchSize;

	private final JdbcParametersList jdbcParameters;
	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelectOperation;

	/**
	 * @param domainBatchSize The maximum number of entities we will initialize for each load
	 */
	public EntityBatchLoaderInPredicate(
			int domainBatchSize,
			EntityMappingType entityDescriptor,
			LoadQueryInfluencers loadQueryInfluencers) {
		super( entityDescriptor, loadQueryInfluencers );
		this.domainBatchSize = domainBatchSize;
		int idColumnCount =
				entityDescriptor.getEntityPersister().getIdentifierType()
						.getColumnSpan( sessionFactory .getRuntimeMetamodels());
		this.sqlBatchSize =
				sessionFactory.getJdbcServices().getDialect().getBatchLoadSizingStrategy()
						.determineOptimalBatchLoadSize( idColumnCount, domainBatchSize, false );

		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf(
					"Batch fetching `%s` entity using padded IN-list : %s (%s)",
					entityDescriptor.getEntityName(),
					domainBatchSize,
					sqlBatchSize
			);
		}

		final EntityIdentifierMapping identifierMapping = getLoadable().getIdentifierMapping();

		final int expectedNumberOfParameters = identifierMapping.getJdbcTypeCount() * sqlBatchSize;
		final JdbcParametersList.Builder jdbcParametersBuilder = JdbcParametersList.newBuilder( expectedNumberOfParameters );
		sqlAst = LoaderSelectBuilder.createSelect(
				getLoadable(),
				// null here means to select everything
				null,
				identifierMapping,
				null,
				sqlBatchSize,
				loadQueryInfluencers,
				LockOptions.NONE,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		this.jdbcParameters = jdbcParametersBuilder.build();
		assert jdbcParameters.size() == expectedNumberOfParameters;

		jdbcSelectOperation = sessionFactory.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildSelectTranslator( sessionFactory, sqlAst )
				.translate( JdbcParameterBindings.NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public int getDomainBatchSize() {
		return domainBatchSize;
	}

	public int getSqlBatchSize() {
		return sqlBatchSize;
	}

	@Override
	public final T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( pkValue, null, lockOptions, readOnly, session );
	}

	protected Object[] resolveIdsToInitialize(Object id, SharedSessionContractImplementor session) {
		return session.getPersistenceContextInternal().getBatchFetchQueue()
				.getBatchLoadableEntityIds( getLoadable(), id, domainBatchSize );
	}

	@Override
	protected void initializeEntities(
			Object[] idsToInitialize,
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
			MULTI_KEY_LOAD_LOGGER.debugf( "Ids to batch-fetch initialize (`%s#%s`) %s",
					getLoadable().getEntityName(), pkValue, Arrays.toString(idsToInitialize) );
		}
		final EntityIdentifierMapping identifierMapping = getLoadable().getIdentifierMapping();
		final MultiKeyLoadChunker<Object> chunker = new MultiKeyLoadChunker<>(
				sqlBatchSize,
				identifierMapping.getJdbcTypeCount(),
				identifierMapping,
				jdbcParameters,
				sqlAst,
				jdbcSelectOperation
		);

		final BatchFetchQueue batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();
		final EntityPersister persister = getLoadable().getEntityPersister();

		chunker.processChunks(
				idsToInitialize,
				sqlBatchSize,
				(jdbcParameterBindings, session1) -> {
					// Create a RegistrationHandler for handling any subselect fetches we encounter handling this chunk
					return new SingleIdExecutionContext(
							pkValue,
							entityInstance,
							getLoadable().getRootEntityDescriptor(),
							readOnly,
							lockOptions,
							SubselectFetch.createRegistrationHandler(
									batchFetchQueue,
									sqlAst,
									jdbcParameters,
									jdbcParameterBindings
							),
							session
					);
				},
				(key, relativePosition, absolutePosition) -> {
					if ( key != null ) {
						batchFetchQueue.removeBatchLoadableEntityKey( session.generateEntityKey( key, persister ) );
					}
				},
				(startIndex) -> {
					if ( MULTI_KEY_LOAD_LOGGER.isDebugEnabled() ) {
						MULTI_KEY_LOAD_LOGGER.debugf(
								"Processing entity batch-fetch chunk (`%s#%s`) %s - %s",
								getLoadable().getEntityName(),
								pkValue,
								startIndex,
								startIndex + ( sqlBatchSize - 1 )
						);
					}
				},
				(startIndex, nonNullElementCount) -> {
				},
				session
		);
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"EntityBatchLoaderInPredicate(%s [%s (%s)])",
				getLoadable().getEntityName(),
				domainBatchSize,
				sqlBatchSize
		);
	}
}
