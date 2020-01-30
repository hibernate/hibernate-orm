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

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
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
public class NaturalIdLoaderStandardImpl<T> implements NaturalIdLoader<T> {
	private final EntityPersister entityDescriptor;
	private	final NaturalIdMapping naturalIdMapping;

	public NaturalIdLoaderStandardImpl(EntityPersister entityDescriptor) {
		this.entityDescriptor = entityDescriptor;
		this.naturalIdMapping = entityDescriptor.getNaturalIdMapping();

		if ( ! entityDescriptor.hasNaturalIdentifier() ) {
			throw new HibernateException( "Entity does not define natural-id : " + entityDescriptor.getEntityName() );
		}

		// todo (6.0) : account for nullable attributes that are part of the natural-id (is-null-or-equals)
		// todo (6.0) : cache the SQL AST and JdbcParameter list
	}

	@Override
	public EntityPersister getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object naturalIdToLoad, LoadOptions options, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				Collections.emptyList(),
				naturalIdMapping,
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		for ( int i = 0; i < naturalIdMapping.getNaturalIdAttributes().size(); i++ ) {
			final SingularAttributeMapping attrMapping = naturalIdMapping.getNaturalIdAttributes().get( i );
			attrMapping.visitJdbcValues(
					naturalIdToLoad,
					Clause.WHERE,
					(jdbcValue, jdbcMapping) -> {
						assert jdbcParamItr.hasNext();
						final JdbcParameter jdbcParam = jdbcParamItr.next();
						jdbcParamBindings.addBinding(
								jdbcParam,
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
		}

		//noinspection unchecked
		final List<T> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
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
				row -> (T) row[0],
				true
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Loading by natural-id returned more that one row : %s",
							entityDescriptor.getEntityName()
					)
			);
		}

		return results.get( 0 );
	}

	@Override
	public Object[] resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				naturalIdMapping.getNaturalIdAttributes(),
				entityDescriptor.getIdentifierMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		entityDescriptor.getIdentifierMapping().visitJdbcValues(
				id,
				Clause.WHERE,
				(value, type) -> {
					assert jdbcParamItr.hasNext();
					final JdbcParameter jdbcParam = jdbcParamItr.next();
					jdbcParamBindings.addBinding(
							jdbcParam,
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


		final List<Object[]> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
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
				row -> row,
				true
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Resolving id to natural-id returned more that one row : %s #%s",
							entityDescriptor.getEntityName(),
							id
					)
			);
		}

		return results.get( 0 );
	}

	@Override
	public Object resolveNaturalIdToId(
			Object[] naturalIdValues,
			SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor,
				Collections.singletonList( entityDescriptor.getIdentifierMapping() ),
				naturalIdMapping,
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		for ( int i = 0; i < naturalIdMapping.getNaturalIdAttributes().size(); i++ ) {
			final SingularAttributeMapping attrMapping = naturalIdMapping.getNaturalIdAttributes().get( i );
			attrMapping.visitJdbcValues(
					naturalIdValues[i],
					Clause.WHERE,
					(jdbcValue, jdbcMapping) -> {
						assert jdbcParamItr.hasNext();
						jdbcParamBindings.addBinding(
								jdbcParamItr.next(),
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
		}
		assert !jdbcParamItr.hasNext();

		final List<Object[]> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParamBindings,
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
				row -> row,
				true
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Resolving natural-id to id returned more that one row : %s [%s]",
							entityDescriptor.getEntityName(),
							StringHelper.join( ", ", naturalIdValues )
					)
			);
		}

		return results.get( 0 );
	}
}
