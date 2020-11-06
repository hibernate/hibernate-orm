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
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
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
 * NaturalIdLoader for simple natural-ids
 */
public class SimpleNaturalIdLoader<T> extends AbstractNaturalIdLoader<T> {

	public SimpleNaturalIdLoader(
			SimpleNaturalIdMapping naturalIdMapping,
			NaturalIdPreLoadListener preLoadListener,
			NaturalIdPostLoadListener postLoadListener,
			EntityMappingType entityDescriptor,
			MappingModelCreationProcess creationProcess) {
		super( naturalIdMapping, preLoadListener, postLoadListener, entityDescriptor, creationProcess );
	}

	@Override
	protected SimpleNaturalIdMapping naturalIdMapping() {
		return (SimpleNaturalIdMapping) super.naturalIdMapping();
	}

	@Override
	protected void applyNaturalIdAsJdbcParameters(
			Object naturalIdToLoad,
			List<JdbcParameter> jdbcParameters,
			JdbcParameterBindings jdbcParamBindings,
			SharedSessionContractImplementor session) {
		assert jdbcParameters.size() == 1;

		final Object bindableValue = naturalIdMapping().normalizeValue( naturalIdToLoad, session );

		final SingularAttributeMapping attributeMapping = naturalIdMapping().getNaturalIdAttributes().get( 0 );
		attributeMapping.visitJdbcValues(
				bindableValue,
				Clause.WHERE,
				(jdbcValue, jdbcMapping) -> {
					final JdbcParameter jdbcParam = jdbcParameters.get( 0 );
					jdbcParamBindings.addBinding(
							jdbcParam,
							new JdbcParameterBindingImpl( jdbcMapping, jdbcValue )
					);
				},
				session
		);
	}

	@Override
	protected Object resolveNaturalIdBindValue(Object naturalIdToLoad, SharedSessionContractImplementor session) {
		return naturalIdMapping().normalizeValue( naturalIdToLoad, session );
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
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		entityDescriptor().getIdentifierMapping().visitJdbcValues(
				id,
				Clause.WHERE,
				(value, type) -> {
					assert jdbcParamItr.hasNext();
					final JdbcParameter jdbcParam = jdbcParamItr.next();
					jdbcParamBindings.addBinding(
							jdbcParam,
							new JdbcParameterBindingImpl( type, value )
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
							entityDescriptor().getEntityName(),
							id
					)
			);
		}

		return results.get( 0 );
	}

	@Override
	protected boolean isSimple() {
		return true;
	}

	@Override
	public Object resolveNaturalIdToId(
			Object naturalIdValue,
			SharedSessionContractImplementor session) {
		final Object bindValue = naturalIdMapping().normalizeValue( naturalIdValue, session );

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
		assert jdbcParameters.size() == 1;

		final JdbcSelect jdbcSelect = sqlAstTranslatorFactory.buildSelectTranslator( sessionFactory ).translate( sqlSelect );

		final JdbcParameterBindings jdbcParamBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final Iterator<JdbcParameter> jdbcParamItr = jdbcParameters.iterator();

		final SingularAttributeMapping attributeMapping = naturalIdMapping().getAttribute();
		attributeMapping.visitJdbcValues(
				bindValue,
				Clause.WHERE,
				(jdbcValue, jdbcMapping) -> {
					assert jdbcParamItr.hasNext();
					jdbcParamBindings.addBinding(
							jdbcParamItr.next(),
							new JdbcParameterBindingImpl( jdbcMapping, jdbcValue )
					);
				},
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
}
