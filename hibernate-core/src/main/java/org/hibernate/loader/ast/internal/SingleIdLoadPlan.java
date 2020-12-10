/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryOptionsAdapter;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

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
	private final ModelPart restrictivePart;
	private final SelectStatement sqlAst;
	private final List<JdbcParameter> jdbcParameters;

	public SingleIdLoadPlan(
			ModelPart restrictivePart,
			SelectStatement sqlAst,
			List<JdbcParameter> jdbcParameters) {
		this.restrictivePart = restrictivePart;
		this.sqlAst = sqlAst;
		this.jdbcParameters = jdbcParameters;
	}

	@Override
	public Loadable getLoadable() {
		return null;
	}

	@Override
	public ModelPart getRestrictivePart() {
		return restrictivePart;
	}

	@Override
	public SelectStatement getSqlAst() {
		return sqlAst;
	}

	T load(
			Object restrictedValue,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		return load( restrictedValue, lockOptions, null, readOnly, session );
	}

	T load(
			Object restrictedValue,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		return load( restrictedValue, lockOptions, null, null, session );
	}

	T load(
			Object restrictedValue,
			LockOptions lockOptions,
			Object entityInstance,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

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

		final List list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
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
						return new QueryOptionsAdapter() {
							@Override
							public Boolean isReadOnly() {
								return readOnly;
							}
						};
					}

					@Override
					public QueryParameterBindings getQueryParameterBindings() {
						return QueryParameterBindings.NO_PARAM_BINDINGS;
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {
						};
					}
				},
				RowTransformerPassThruImpl.instance(),
				true
		);

		if ( list.isEmpty() ) {
			return null;
		}

		//noinspection unchecked
		return (T) list.get( 0 );
	}
}
