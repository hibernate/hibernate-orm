/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.MultiIdEntityLoader;
import org.hibernate.loader.spi.MultiIdLoaderSelectors;
import org.hibernate.loader.spi.MultiLoadOptions;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.consume.spi.SqlAstSelectToJdbcSelectConverter;
import org.hibernate.sql.ast.consume.spi.StandardParameterBindingContext;
import org.hibernate.sql.ast.produce.metamodel.internal.SelectByEntityIdentifierBuilder;
import org.hibernate.sql.ast.produce.spi.SqlAstSelectDescriptor;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.exec.internal.StandardJdbcParameterImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.ParameterBindingContext;

/**
 * @author Steve Ebersole
 */
public class StandardMultiIdEntityLoader<J>
		implements MultiIdEntityLoader<J> {
	private final EntityTypeDescriptor<J> entityDescriptor;
	private final MultiIdLoaderSelectors selectors;

	public StandardMultiIdEntityLoader(EntityTypeDescriptor entityDescriptor, MultiIdLoaderSelectors selectors) {
		this.entityDescriptor = entityDescriptor;
		this.selectors = selectors;
	}

	@Override
	public EntityTypeDescriptor<J> getEntityDescriptor() {
		return entityDescriptor;
	}

	@Override
	public List<J> load(
			Object[] ids,
			MultiLoadOptions options,
			SharedSessionContractImplementor session) {

		// todo (6.0) : account for batch size, if one

		final SelectByEntityIdentifierBuilder selectBuilder = new SelectByEntityIdentifierBuilder(
				session.getSessionFactory(),
				entityDescriptor
		);

		final SqlAstSelectDescriptor selectDescriptor = selectBuilder
				.generateSelectStatement( ids.length, session.getLoadQueryInfluencers(), options.getLockOptions() );


		final JdbcSelect jdbcSelect = SqlAstSelectToJdbcSelectConverter.interpret(
				selectDescriptor,
				session.getSessionFactory()
		);


		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl();
		for ( Object id : ids ) {
			entityDescriptor.getHierarchy().getIdentifierDescriptor().dehydrate(
					id,
					(jdbcValue, type, boundColumn) -> jdbcParameterBindings.addBinding(
							new StandardJdbcParameterImpl(
									jdbcParameterBindings.getBindings().size(),
									type,
									Clause.WHERE,
									session.getFactory().getTypeConfiguration()
							),
							new JdbcParameterBinding() {
								@Override
								public SqlExpressableType getBindType() {
									return type;
								}

								@Override
								public Object getBindValue() {
									return jdbcValue;
								}
							}
					),
					Clause.WHERE,
					session
			);
		}

		final ParameterBindingContext parameterBindingContext = new StandardParameterBindingContext(
				session.getFactory(),
				QueryParameterBindings.NO_PARAM_BINDINGS,
				Collections.emptyList()
		);


		return JdbcSelectExecutorStandardImpl.INSTANCE.list(
				jdbcSelect,
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
					public ParameterBindingContext getParameterBindingContext() {
						return parameterBindingContext;
					}

					@Override
					public JdbcParameterBindings getJdbcParameterBindings() {
						return jdbcParameterBindings;
					}

					@Override
					public Callback getCallback() {
						return afterLoadAction -> {};
					}
				},
				RowTransformerSingularReturnImpl.instance()
		);
	}

	@Override
	public EntityTypeDescriptor<J> getLoadedNavigable() {
		return entityDescriptor;
	}
}
