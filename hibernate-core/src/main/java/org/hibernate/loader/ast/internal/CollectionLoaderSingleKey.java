/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * Main implementation of CollectionLoader for handling a load of a single collection-key
 *
 * @author Steve Ebersole
 */
public class CollectionLoaderSingleKey implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;

	private final int keyJdbcCount;

	private final SelectStatement sqlAst;
	private final List<JdbcParameter> jdbcParameters;

	public CollectionLoaderSingleKey(
			PluralAttributeMapping attributeMapping,
			LoadQueryInfluencers influencers,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;

		this.keyJdbcCount = attributeMapping.getKeyDescriptor().getJdbcTypeCount( sessionFactory.getTypeConfiguration() );

		this.jdbcParameters = new ArrayList<>();
		this.sqlAst = LoaderSelectBuilder.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				1,
				influencers,
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);
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

	public List<JdbcParameter> getJdbcParameters() {
		return jdbcParameters;
	}

	@Override
	public PersistentCollection load(Object key, SharedSessionContractImplementor session) {
		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), key );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( keyJdbcCount );

		final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

		attributeMapping.getKeyDescriptor().visitJdbcValues(
				key,
				Clause.WHERE,
				(value, type) -> {
					assert paramItr.hasNext();
					final JdbcParameter parameter = paramItr.next();
					jdbcParameterBindings.addBinding(
							parameter,
							new JdbcParameterBinding() {
								@Override
								public JdbcMapping getBindType() {
									return type;
								}

								@Override
								public Object getBindValue() {
									return value;
								}
							}
					);
				},
				session
		);
		assert !paramItr.hasNext();

		jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public CollectionKey getCollectionKey() {
						return collectionKey;
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

		return session.getPersistenceContext().getCollection( collectionKey );
	}
}
