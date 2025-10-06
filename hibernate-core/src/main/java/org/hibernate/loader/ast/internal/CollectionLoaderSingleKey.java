/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/**
 * Main implementation of CollectionLoader for handling a load of a single collection-key
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderSingleKey implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;

	private final int keyJdbcCount;

	private final SelectStatement sqlAst;
	private final JdbcOperationQuerySelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	public CollectionLoaderSingleKey(
			PluralAttributeMapping attributeMapping,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;

		keyJdbcCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount();
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();

		sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				1,
				influencers,
				new LockOptions(),
				jdbcParametersBuilder::add,
				sessionFactory
		);

		final var querySpec = sqlAst.getQueryPart().getFirstQuerySpec();
		final var tableGroup = querySpec.getFromClause().getRoots().get( 0 );
		attributeMapping.applySoftDeleteRestrictions( tableGroup, querySpec::applyPredicate );

		jdbcParameters = jdbcParametersBuilder.build();
		jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildSelectTranslator( sessionFactory, sqlAst )
						.translate( NO_BINDINGS, QueryOptions.NONE );
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return getAttributeMapping();
	}

	public PluralAttributeMapping getAttributeMapping() {
		return attributeMapping;
	}

	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	public JdbcParametersList getJdbcParameters() {
		return jdbcParameters;
	}

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		final var collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), key );

		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final var subSelectFetchableKeysHandler = SubselectFetch.createRegistrationHandler(
				session.getPersistenceContext().getBatchFetchQueue(),
				sqlAst,
				jdbcParameters,
				jdbcParameterBindings
		);

		session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new CollectionLoaderSingleKeyExecutionContext( session, collectionKey, subSelectFetchableKeysHandler ),
				RowTransformerStandardImpl.instance(),
				ListResultsConsumer.UniqueSemantic.FILTER
		);

		return session.getPersistenceContext().getCollection( collectionKey );
	}

	private static class CollectionLoaderSingleKeyExecutionContext extends BaseExecutionContext {
		private final CollectionKey collectionKey;
		private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;

		CollectionLoaderSingleKeyExecutionContext(
				SharedSessionContractImplementor session,
				CollectionKey collectionKey,
				SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler) {
			super( session );
			this.collectionKey = collectionKey;
			this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		}

		@Override
		public CollectionKey getCollectionKey() {
			return collectionKey;
		}

		@Override
		public void registerLoadingEntityHolder(EntityHolder holder) {
			subSelectFetchableKeysHandler.addKey( holder );
		}
	}
}
