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
import org.hibernate.loader.NaturalIdPostLoadListener;
import org.hibernate.loader.NaturalIdPreLoadListener;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.Callback;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;

/**
 * Base support for NaturalIdLoader implementations
 */
public abstract class AbstractNaturalIdLoader<T> implements NaturalIdLoader<T> {

	// todo (6.0) : account for nullable attributes that are part of the natural-id (is-null-or-equals)
	// todo (6.0) : cache the SQL AST and JdbcParameter list

	private final NaturalIdMapping naturalIdMapping;
	private final EntityMappingType entityDescriptor;

	private final NaturalIdPreLoadListener preLoadListener;
	private final NaturalIdPostLoadListener postLoadListener;

	public AbstractNaturalIdLoader(
			NaturalIdMapping naturalIdMapping,
			NaturalIdPreLoadListener preLoadListener,
			NaturalIdPostLoadListener postLoadListener,
			EntityMappingType entityDescriptor,
			MappingModelCreationProcess creationProcess) {
		this.naturalIdMapping = naturalIdMapping;
		this.preLoadListener = preLoadListener;
		this.postLoadListener = postLoadListener;
		this.entityDescriptor = entityDescriptor;
	}

	protected EntityMappingType entityDescriptor() {
		return entityDescriptor;
	}

	protected NaturalIdMapping naturalIdMapping() {
		return naturalIdMapping;
	}

	@Override
	public Loadable getLoadable() {
		return entityDescriptor();
	}

	@Override
	public T load(Object naturalIdValue, LoadOptions options, SharedSessionContractImplementor session) {
		final Object bindValue = resolveNaturalIdBindValue( naturalIdValue, session );
		preLoadListener.startingLoadByNaturalId( entityDescriptor, bindValue, session );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				Collections.emptyList(),
				naturalIdMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				options.getLockOptions(),
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );

		applyNaturalIdAsJdbcParameters( bindValue, jdbcParameters, jdbcParamBindings, session );

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

		final T result = results.get( 0 );
		postLoadListener.completedLoadByNaturalId( entityDescriptor, result, session );

		return result;
	}

	protected abstract Object resolveNaturalIdBindValue(Object naturalIdToLoad, SharedSessionContractImplementor session);

	protected abstract void applyNaturalIdAsJdbcParameters(
			Object naturalIdToLoad,
			List<JdbcParameter> jdbcParameters,
			JdbcParameterBindings jdbcParamBindings, SharedSessionContractImplementor session);

	@Override
	public Object resolveNaturalIdToId(Object naturalIdValue, SharedSessionContractImplementor session) {
		final Object bindValue = resolveNaturalIdBindValue( naturalIdValue, session );

		final SessionFactoryImplementor sessionFactory = session.getFactory();
		final JdbcServices jdbcServices = sessionFactory.getJdbcServices();
		final JdbcEnvironment jdbcEnvironment = jdbcServices.getJdbcEnvironment();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = jdbcEnvironment.getSqlAstTranslatorFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				Collections.singletonList( entityDescriptor().getIdentifierMapping() ),
				naturalIdMapping(),
				null,
				1,
				session.getLoadQueryInfluencers(),
				LockOptions.READ,
				jdbcParameters::add,
				sessionFactory
		);

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		applyNaturalIdAsJdbcParameters(
				bindValue,
				jdbcParameters,
				jdbcParamBindings,
				session
		);

		final List<?> results = session.getFactory().getJdbcServices().getJdbcSelectExecutor().list(
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
				row -> row[0],
				true
		);

		if ( results.size() > 1 ) {
			throw new HibernateException(
					String.format(
							"Resolving natural-id to id returned more that one row : %s [%s]",
							entityDescriptor().getEntityName(),
							bindValue
					)
			);
		}

		return results.get( 0 );
	}

	@Override
	public Object resolveIdToNaturalId(Object id, SharedSessionContractImplementor session) {
		final SessionFactoryImplementor sessionFactory = session.getFactory();

		final List<JdbcParameter> jdbcParameters = new ArrayList<>();
		final SelectStatement sqlSelect = LoaderSelectBuilder.createSelect(
				entityDescriptor(),
				naturalIdMapping().getNaturalIdAttributes(),
				entityDescriptor().getIdentifierMapping(),
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

		int offset = jdbcParamBindings.registerParametersForEachJdbcValue(
				id,
				Clause.WHERE,
				entityDescriptor().getIdentifierMapping(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

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
							entityDescriptor().getEntityName(),
							id
					)
			);
		}

		final Object[] objects = results.get( 0 );
		if ( isSimple() ) {
			return objects[0];
		}

		return objects;
	}

	protected abstract boolean isSimple();
}
