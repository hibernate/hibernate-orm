/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.hibernate.LockOptions;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.loader.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.spi.DomainResult;

/**
 * A one-time use CollectionLoader for applying a sub-select fetch
 *
 * @author Steve Ebersole
 */
public class SubSelectFetchCollectionLoader implements CollectionLoader {
	private final PluralAttributeMapping attributeMapping;
	private final DomainResult cachedDomainResult;
	private final SubselectFetch subselect;

	private final SelectStatement sqlAst;
	private final java.util.List<JdbcParameter> jdbcParameters;

	public SubSelectFetchCollectionLoader(
			PluralAttributeMapping attributeMapping,
			DomainResult cachedDomainResult,
			SubselectFetch subselect,
			SharedSessionContractImplementor session) {
		this.attributeMapping = attributeMapping;
		this.cachedDomainResult = cachedDomainResult;
		this.subselect = subselect;

		jdbcParameters = new ArrayList<>();

		sqlAst = MetamodelSelectBuilderProcess.createSelect(
				attributeMapping,
				null,
				attributeMapping.getKeyDescriptor(),
				null,
				subselect.getResult().size(),
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
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

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				attributeMapping.getKeyDescriptor().getJdbcTypeCount( sessionFactory.getTypeConfiguration() )
		);

		for ( EntityKey key : subselect.getResult() ) {
			final Iterator<JdbcParameter> paramItr = jdbcParameters.iterator();

			attributeMapping.getKeyDescriptor().visitJdbcValues(
					key.getIdentifierValue(),
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
		}

		JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
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
				RowTransformerPassThruImpl.instance()
		);

		final CollectionKey collectionKey = new CollectionKey( attributeMapping.getCollectionDescriptor(), triggerKey );
		return session.getPersistenceContext().getCollection( collectionKey );
	}
}
