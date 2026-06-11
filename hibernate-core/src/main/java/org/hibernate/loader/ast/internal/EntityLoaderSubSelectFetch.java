/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.EntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
 * A one-time use {@link EntityLoader} for applying a subselect fetch to a to-one association.
 */
public class EntityLoaderSubSelectFetch implements EntityLoader {
	private final EntityMappingType entityMapping;
	private final SubselectFetch subselect;

	private final SelectStatement sqlAst;

	public EntityLoaderSubSelectFetch(
			EntityMappingType entityMapping,
			ToOneAttributeMapping attributeMapping,
			SubselectFetch subselect,
			SharedSessionContractImplementor session) {
		this.entityMapping = entityMapping;
		this.subselect = subselect;

		sqlAst = LoaderSelectBuilder.createSubSelectFetchSelect(
				entityMapping,
				attributeMapping,
				subselect,
				null,
				session.getLoadQueryInfluencers(),
				new LockOptions(),
				jdbcParameter -> {},
				new SqlAliasBaseManager(),
				session.getFactory()
		);
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityMapping;
	}

	public void load(SharedSessionContractImplementor session) {
		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var batchFetchQueue = session.getPersistenceContextInternal().getBatchFetchQueue();

		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment()
						.getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqlAst )
						.translate( subselect.getLoadingJdbcParameterBindings(), QueryOptions.NONE );

		final var subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				subselect.getLoadingJdbcParameters(),
				subselect.getLoadingJdbcParameterBindings()
		);

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				subselect.getLoadingJdbcParameterBindings(),
				new ExecutionContextWithSubselectFetchHandler( session, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);
	}
}
