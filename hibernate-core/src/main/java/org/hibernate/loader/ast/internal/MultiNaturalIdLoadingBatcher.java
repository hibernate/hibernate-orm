/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

/**
 * Batch support for natural-id multi loading
 */
public class MultiNaturalIdLoadingBatcher {

	@FunctionalInterface
	interface KeyValueResolver {
		/**
		 * Resolve the supported forms of representing the natural-id value to
		 * the "true" form - single value for simple natural-ids and an array for
		 * compound natural-ids.
		 *
		 * Generally delegates to {@link org.hibernate.metamodel.mapping.NaturalIdMapping#normalizeInput}
		 */
		Object resolveKeyToLoad(Object incoming, SharedSessionContractImplementor session);
	}

	private final EntityMappingType entityDescriptor;

	private final SelectStatement sqlSelect;
	private final JdbcParametersList jdbcParameters;

	private final KeyValueResolver keyValueResolver;

	private final JdbcOperationQuerySelect jdbcSelect;

	private final LockOptions lockOptions;

	public MultiNaturalIdLoadingBatcher(
			EntityMappingType entityDescriptor,
			ModelPart restrictedPart,
			int batchSize,
			KeyValueResolver keyValueResolver,
			LoadQueryInfluencers loadQueryInfluencers,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();

		sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				// return the full entity rather than parts
				null,
				restrictedPart,
				// no "cached" DomainResult
				null,
				batchSize,
				loadQueryInfluencers,
				lockOptions,
				jdbcParametersBuilder::add,
				sessionFactory
		);
		this.jdbcParameters = jdbcParametersBuilder.build();

		this.keyValueResolver = keyValueResolver;

		this.jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqlSelect )
						.translate( null, new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						} );
		this.lockOptions = lockOptions;
	}

	public <E> List<E> multiLoad(Object[] naturalIdValues, SharedSessionContractImplementor session) {
		final ArrayList<E> multiLoadResults = arrayList( naturalIdValues.length );
		final var jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );

		int offset = 0;
		int size = 0;

		for ( Object naturalIdValue : naturalIdValues ) {
			final Object bindValue = keyValueResolver.resolveKeyToLoad( naturalIdValue, session );
			if ( bindValue != null ) {
				offset += jdbcParamBindings.registerParametersForEachJdbcValue(
						bindValue,
						offset,
						entityDescriptor.getNaturalIdMapping(),
						jdbcParameters,
						session
				);
				size++;
			}

			if ( offset == jdbcParameters.size() ) {
				// we've hit the batch mark
				multiLoadResults.addAll( performLoad( jdbcParamBindings, session, size ) );
				jdbcParamBindings.clear();
				offset = 0;
				size = 0;
			}
		}

		if ( offset != 0 ) {
			while ( offset != jdbcParameters.size() ) {
				// pad the remaining parameters with null
				offset += jdbcParamBindings.registerParametersForEachJdbcValue(
						null,
						offset,
						entityDescriptor.getNaturalIdMapping(),
						jdbcParameters,
						session
				);
				size++;
			}
			multiLoadResults.addAll( performLoad( jdbcParamBindings, session, size ) );
		}

		return multiLoadResults;
	}

	private <E> List<E> performLoad(
			JdbcParameterBindings jdbcParamBindings,
			SharedSessionContractImplementor session,
			int size) {
		final var subSelectFetchableKeysHandler =
				session.getLoadQueryInfluencers()
					.hasSubselectLoadableCollections( entityDescriptor.getEntityPersister() )
						? SubselectFetch.createRegistrationHandler(
								session.getPersistenceContext().getBatchFetchQueue(),
								sqlSelect,
								jdbcParameters,
								jdbcParamBindings
						)
						: null;
		return session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler, false, lockOptions ),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				size
		);
	}

}
