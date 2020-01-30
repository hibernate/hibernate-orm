/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleUniqueKeyEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinding;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * @author Steve Ebersole
 */
public class SingleUniqueKeyEntityLoaderStandard<T> implements SingleUniqueKeyEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final SingularAttributeMapping uniqueKeyAttribute;

	public SingleUniqueKeyEntityLoaderStandard(
			EntityMappingType entityDescriptor,
			SingularAttributeMapping uniqueKeyAttribute) {
		this.entityDescriptor = entityDescriptor;
		this.uniqueKeyAttribute = uniqueKeyAttribute;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(
			Object ukValue,
			LockOptions lockOptions,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		// todo (6.0) : cache the SQL AST and JdbcParameters
		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				Collections.emptyList(),
				uniqueKeyAttribute,
				null,
				1,
				LoadQueryInfluencers.NONE,
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();
		uniqueKeyAttribute.visitJdbcValues(
				ukValue,
				Clause.WHERE,
				(jdbcValue, jdbcMapping) -> {
					assert jdbcParamItr.hasNext();
					final JdbcParameter jdbcParameter = jdbcParamItr.next();
					jdbcParameterBindings.addBinding(
							jdbcParameter,
							new JdbcParameterBinding() {
								@Override
								public JdbcMapping getBindType() {
									return jdbcMapping;
								}

								@Override
								public Object getBindValue() {
									return jdbcValue;
								}
							}
					);
				},
				session
		);

		final List<Object> list = sessionFactory.getJdbcServices().getJdbcSelectExecutor().list(
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
						return afterLoadAction -> {
						};
					}
				},
				row -> row[0],
				true
		);

		assert list.size() == 1;

		//noinspection unchecked
		return (T) list.get( 0 );
	}

	@Override
	public Object resolveId(Object ukValue, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		// todo (6.0) : cache the SQL AST and JdbcParameters
		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlAst = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				Collections.singletonList( entityDescriptor.getIdentifierMapping() ),
				uniqueKeyAttribute,
				null,
				1,
				LoadQueryInfluencers.NONE,
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlAst );

		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();
		uniqueKeyAttribute.visitJdbcValues(
				ukValue,
				Clause.WHERE,
				(jdbcValue, jdbcMapping) -> {
					assert jdbcParamItr.hasNext();
					final JdbcParameter jdbcParameter = jdbcParamItr.next();
					jdbcParameterBindings.addBinding(
							jdbcParameter,
							new JdbcParameterBinding() {
								@Override
								public JdbcMapping getBindType() {
									return jdbcMapping;
								}

								@Override
								public Object getBindValue() {
									return jdbcValue;
								}
							}
					);
				},
				session
		);

		final List<Object> list = sessionFactory.getJdbcServices().getJdbcSelectExecutor().list(
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
						return afterLoadAction -> {
						};
					}
				},
				row -> row[0],
				true
		);

		assert list.size() == 1;

		return list.get( 0 );
	}
}
