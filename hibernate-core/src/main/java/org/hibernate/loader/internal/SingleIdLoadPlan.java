/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;

/**
 * @author Steve Ebersole
 */
class SingleIdLoadPlan<T> {
	private final ModelPart restrictivePart;
	private final MetamodelSelectBuilderProcess.SqlAstDescriptor sqlAstDescriptor;

	public SingleIdLoadPlan(
			ModelPart restrictivePart,
			MetamodelSelectBuilderProcess.SqlAstDescriptor sqlAstDescriptor) {
		this.restrictivePart = restrictivePart;
		this.sqlAstDescriptor = sqlAstDescriptor;
	}

	T load(Object restrictedValue, LockOptions lockOptions, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectConverter( sessionFactory ).interpret( sqlAstDescriptor.getSqlAst() );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				restrictivePart.getJdbcTypeCount( sessionFactory.getTypeConfiguration() )
		);

		final Iterator<JdbcParameter> paramItr = sqlAstDescriptor.getJdbcParameters().iterator();

		restrictivePart.visitJdbcValues(
				restrictedValue,
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

		final List list = JdbcSelectExecutorStandardImpl.INSTANCE.list(
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

		if ( list.isEmpty() ) {
			return null;
		}

		//noinspection unchecked
		return (T) list.get( 0 );
	}
}
