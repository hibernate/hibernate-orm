/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * A one-time use CollectionLoader for applying a sub-select fetch
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderSubSelectFetch implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;
	private final SubselectFetch subselect;

	private final SelectStatement sqlAst;

	public CollectionLoaderSubSelectFetch(
			PluralAttributeMapping attributeMapping,
			DomainResult cachedDomainResult,
			SubselectFetch subselect,
			SharedSessionContractImplementor session) {
		this.attributeMapping = attributeMapping;
		this.subselect = subselect;

		sqlAst = LoaderSelectBuilder.createSubSelectFetchSelect(
				attributeMapping,
				subselect,
				cachedDomainResult,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameter -> {},
				session.getFactory()
		);
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return attributeMapping;
	}

	@Override
	public PersistentCollection load(Object triggerKey, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				subselect.getLoadingJdbcParameterBindings(),
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return QueryOptions.NONE;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return null;
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);

		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), triggerKey );
		return session.getPersistenceContext().getCollection( collectionKey );
	}
}
