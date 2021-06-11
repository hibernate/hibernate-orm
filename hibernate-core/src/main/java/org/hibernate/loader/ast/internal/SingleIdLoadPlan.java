/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.CallbackImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * todo (6.0) : this can generically define a load-by-uk as well.  only the SQL AST and `restrictivePart` vary and they are passed as ctor args
 *
 * Describes a plan for loading an entity by identifier.
 *
 * @implNote Made up of (1) a SQL AST for the SQL SELECT and (2) the `ModelPart` used as the restriction
 *
 * @author Steve Ebersole
 */
public class SingleIdLoadPlan<T> implements SingleEntityLoadPlan {
	private final org.hibernate.persister.entity.Loadable persister;
	private final ModelPart restrictivePart;
	private final LockOptions lockOptions;
	private final JdbcSelect jdbcSelect;
	private final List<JdbcParameter> jdbcParameters;

	public SingleIdLoadPlan(
			org.hibernate.persister.entity.Loadable persister,
			ModelPart restrictivePart,
			SelectStatement sqlAst,
			List<JdbcParameter> jdbcParameters,
			LockOptions lockOptions,
			SessionFactoryImplementor sessionFactory) {
		this.persister = persister;
		this.restrictivePart = restrictivePart;
		this.lockOptions = lockOptions.makeCopy();
		this.jdbcParameters = jdbcParameters;
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();
		this.jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory, sqlAst )
				.translate(
						null,
						new QueryOptionsAdapter() {
							@Override
							public LockOptions getLockOptions() {
								return lockOptions;
							}
						}
				);
	}

	@Override
	public Loadable getLoadable() {
		return persister;
	}

	@Override
	public ModelPart getRestrictivePart() {
		return restrictivePart;
	}

	@Override
	public JdbcSelect getJdbcSelect() {
		return jdbcSelect;
	}

	protected RowTransformer<T> getRowTransformer() {
		return RowTransformerPassThruImpl.instance();
	}

	public T load(Object restrictedValue, SharedSessionContractImplementor session) {
		return load( restrictedValue, null, null, false, session );
	}

	public T load(Object restrictedValue, Boolean readOnly, SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, false, session );
	}

	public T load(
			Object restrictedValue,
			Boolean readOnly,
			Boolean singleResultExpected,
			SharedSessionContractImplementor session) {
		return load( restrictedValue, null, readOnly, singleResultExpected, session );
	}

	public T load(
			Object restrictedValue,
			Object entityInstance,
			Boolean readOnly,
			Boolean singleResultExpected,
			SharedSessionContractImplementor session) {
		final int jdbcTypeCount = restrictivePart.getJdbcTypeCount();
		assert jdbcParameters.size() % jdbcTypeCount == 0;

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcTypeCount );
		jdbcSelect.bindFilterJdbcParameters( jdbcParameterBindings );

		int offset = 0;
		while ( offset < jdbcParameters.size() ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					restrictedValue,
					Clause.WHERE,
					offset,
					restrictivePart,
					jdbcParameters,
					session
			);
		}
		assert offset == jdbcParameters.size();
		final QueryOptions queryOptions = new SimpleQueryOptions( lockOptions, readOnly );
		final Callback callback = new CallbackImpl();

		final List<T> list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
				jdbcParameterBindings,
				new ExecutionContext() {
					@Override
					public SharedSessionContractImplementor getSession() {
						return session;
					}

					@Override
					public Object getEntityInstance() {
						return entityInstance;
					}

					@Override
					public Object getEntityId() {
						return restrictedValue;
					}

					@Override
					public QueryOptions getQueryOptions() {
						return queryOptions;
					}

					@Override
					public String getQueryIdentifier(String sql) {
						return sql;
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return callback;
					}

				},
				getRowTransformer(),
				singleResultExpected ? ListResultsConsumer.UniqueSemantic.ASSERT : ListResultsConsumer.UniqueSemantic.FILTER
		);

		if ( list.isEmpty() ) {
			return null;
		}

		final T entity = list.get( 0 );
		if ( persister != null ) {
			callback.invokeAfterLoadActions( session, entity, persister );
		}
		return entity;
	}
}
