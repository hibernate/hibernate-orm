/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 */
public class NativeSelectQueryPlanImpl<R> implements NativeSelectQueryPlan<R> {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<QueryParameterImplementor<?>> parameterList;

	private final JdbcValuesMappingProducer resultSetMapping;
	private final RowTransformer<R> rowTransformer;

	public NativeSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<QueryParameterImplementor<?>> parameterList,
			ResultSetMapping resultSetMapping,
			RowTransformer<R> rowTransformer,
			SessionFactoryImplementor sessionFactory) {
		final ResultSetMappingProcessor processor = new ResultSetMappingProcessor( resultSetMapping, sessionFactory );
		final SQLQueryParser parser = new SQLQueryParser( sql, processor.process(), sessionFactory );
		this.sql = parser.process();
		this.parameterList = parameterList;
		this.resultSetMapping = processor.generateResultMapping( parser.queryHasAliases() );
		this.rowTransformer = rowTransformer != null
				? rowTransformer
				: RowTransformerPassThruImpl.instance();
		if ( affectedTableNames == null ) {
			affectedTableNames = new HashSet<>();
		}
		if ( resultSetMapping != null ) {
			resultSetMapping.addAffectedTableNames( affectedTableNames, sessionFactory );
		}
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return Collections.emptyList();
		}
		final List<JdbcParameterBinder> jdbcParameterBinders;
		final JdbcParameterBindings jdbcParameterBindings;

		final QueryParameterBindings queryParameterBindings = executionContext.getQueryParameterBindings();
		if ( parameterList == null || parameterList.isEmpty() ) {
			jdbcParameterBinders = Collections.emptyList();
			jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
		}
		else {
			jdbcParameterBinders = new ArrayList<>( parameterList.size() );
			jdbcParameterBindings = new JdbcParameterBindingsImpl( parameterList.size() );

			for ( QueryParameterImplementor<?> param : parameterList ) {
				QueryParameterBinding<?> binding = queryParameterBindings.getBinding( param );
				AllowableParameterType<?> type = binding.getBindType();
				if ( type == null ) {
					type = param.getHibernateType();
				}
				if ( type == null ) {
					type = StandardBasicTypes.OBJECT_TYPE;
				}

				final JdbcMapping jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
				final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );

				jdbcParameterBinders.add( jdbcParameter );

				jdbcParameterBindings.addBinding(
						jdbcParameter,
						new JdbcParameterBindingImpl( jdbcMapping, binding.getBindValue() )
				);
			}
		}

		executionContext.getSession().autoFlushIfRequired( affectedTableNames );

		final JdbcSelect jdbcSelect = new JdbcSelect(
				sql,
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames,
				Collections.emptySet()
		);

		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		// TODO: use configurable executor instead?
//		final SharedSessionContractImplementor session = executionContext.getSession();
//		final SessionFactoryImplementor factory = session.getFactory();
//		final JdbcServices jdbcServices = factory.getJdbcServices();
//		return jdbcServices.getJdbcMutationExecutor().execute(
		return executor.list(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				ListResultsConsumer.UniqueSemantic.NONE
		);
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.INSTANCE;
		}
		final List<JdbcParameterBinder> jdbcParameterBinders;
		final JdbcParameterBindings jdbcParameterBindings;

		final QueryParameterBindings queryParameterBindings = executionContext.getQueryParameterBindings();
		if ( parameterList.isEmpty() ) {
			jdbcParameterBinders = Collections.emptyList();
			jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
		}
		else {
			jdbcParameterBinders = new ArrayList<>( parameterList.size() );
			jdbcParameterBindings = new JdbcParameterBindingsImpl( parameterList.size() );

			queryParameterBindings.visitBindings(
					(param, binding) -> {
						AllowableParameterType<?> type = binding.getBindType();
						if ( type == null ) {
							type = param.getHibernateType();
						}
						if ( type == null ) {
							type = StandardBasicTypes.OBJECT_TYPE;
						}

						final JdbcMapping jdbcMapping = ( (BasicValuedMapping) type ).getJdbcMapping();
						final JdbcParameterImpl jdbcParameter = new JdbcParameterImpl( jdbcMapping );

						jdbcParameterBinders.add( jdbcParameter );

						jdbcParameterBindings.addBinding(
								jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, binding.getBindValue() )
						);
					}
			);
		}

		final JdbcSelect jdbcSelect = new JdbcSelect(
				sql,
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames,
				Collections.emptySet()
		);

		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.scroll(
				jdbcSelect,
				scrollMode,
				jdbcParameterBindings,
				executionContext,
				rowTransformer
		);
	}
}
