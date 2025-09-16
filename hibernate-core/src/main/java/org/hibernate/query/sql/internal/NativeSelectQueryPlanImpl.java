/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.spi.SelectQueryPlan;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.ResultsConsumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

/**
 * Standard implementation of {@link SelectQueryPlan} for
 * {@link org.hibernate.query.NativeQuery}, that is, for
 * queries written in SQL.
 *
 * @author Steve Ebersole
 */
public class NativeSelectQueryPlanImpl<R> implements NativeSelectQueryPlan<R> {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<ParameterOccurrence> parameterList;

	private final JdbcValuesMappingProducer resultSetMapping;

	public NativeSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<ParameterOccurrence> parameterList,
			ResultSetMapping resultSetMapping,
			SessionFactoryImplementor sessionFactory) {
		final ResultSetMappingProcessor processor = new ResultSetMappingProcessor( resultSetMapping, sessionFactory );
		final SQLQueryParser parser = new SQLQueryParser( sql, processor.process(), sessionFactory );
		this.sql = parser.process();
		this.parameterList = parameterList;
		this.resultSetMapping = processor.generateResultMapping( parser.queryHasAliases() );
		if ( affectedTableNames == null ) {
			affectedTableNames = new HashSet<>();
		}
		if ( resultSetMapping != null ) {
			resultSetMapping.addAffectedTableNames( affectedTableNames, sessionFactory );
		}
		this.affectedTableNames = affectedTableNames;
	}

	@Override
	public <T> T executeQuery(DomainQueryExecutionContext executionContext, ResultsConsumer<T, R> resultsConsumer) {
		final List<JdbcParameterBinder> jdbcParameterBinders;
		final JdbcParameterBindings jdbcParameterBindings;

		final QueryParameterBindings queryParameterBindings = executionContext.getQueryParameterBindings();
		if ( parameterList == null || parameterList.isEmpty() ) {
			jdbcParameterBinders = emptyList();
			jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
		}
		else {
			jdbcParameterBinders = new ArrayList<>( parameterList.size() );
			jdbcParameterBindings = new JdbcParameterBindingsImpl(
					queryParameterBindings,
					parameterList,
					jdbcParameterBinders,
					executionContext.getSession().getFactory()
			);
		}

		final JdbcOperationQuerySelect jdbcSelect = new JdbcOperationQuerySelect(
				sqlToUse( sql, executionContext ),
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames
		);

		return executionContext.getSession().getJdbcServices().getJdbcSelectExecutor().executeQuery(
				jdbcSelect,
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext ),
				null,
				null,
				-1,
				resultsConsumer
		);
	}

	private static String sqlToUse(String sql, DomainQueryExecutionContext executionContext) {
		final LockOptions lockOptions = executionContext.getQueryOptions().getLockOptions();
		return lockOptions.getLockMode().isPessimistic()
				? executionContext.getSession().getDialect().applyLocksToSql( sql, lockOptions, emptyMap() )
				: sql;
	}

	@Override
	public List<R> performList(DomainQueryExecutionContext executionContext) {
		final QueryOptions queryOptions = executionContext.getQueryOptions();
		if ( queryOptions.getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return emptyList();
		}
		else {
			final List<JdbcParameterBinder> jdbcParameterBinders;
			final JdbcParameterBindings jdbcParameterBindings;
			if ( parameterList == null || parameterList.isEmpty() ) {
				jdbcParameterBinders = emptyList();
				jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
			}
			else {
				jdbcParameterBinders = new ArrayList<>( parameterList.size() );
				jdbcParameterBindings = new JdbcParameterBindingsImpl(
						executionContext.getQueryParameterBindings(),
						parameterList,
						jdbcParameterBinders,
						executionContext.getSession().getFactory()
				);
			}

			final JdbcOperationQuerySelect jdbcSelect = new JdbcOperationQuerySelect(
					sqlToUse( sql, executionContext ),
					jdbcParameterBinders,
					resultSetMapping,
					affectedTableNames
			);

			executionContext.getSession().autoFlushIfRequired( jdbcSelect.getAffectedTableNames() );
			return executionContext.getSession().getJdbcServices().getJdbcSelectExecutor().list(
					jdbcSelect,
					jdbcParameterBindings,
					SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext ),
					null,
					queryOptions.getUniqueSemantic() == null
							? ListResultsConsumer.UniqueSemantic.NEVER
							: queryOptions.getUniqueSemantic()
			);
		}
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.instance();
		}
		else {
			final List<JdbcParameterBinder> jdbcParameterBinders;
			final JdbcParameterBindings jdbcParameterBindings;
			if ( parameterList == null || parameterList.isEmpty() ) {
				jdbcParameterBinders = emptyList();
				jdbcParameterBindings = JdbcParameterBindings.NO_BINDINGS;
			}
			else {
				jdbcParameterBinders = new ArrayList<>( parameterList.size() );
				jdbcParameterBindings = new JdbcParameterBindingsImpl(
						executionContext.getQueryParameterBindings(),
						parameterList,
						jdbcParameterBinders,
						executionContext.getSession().getFactory()
				);
			}

			final JdbcOperationQuerySelect jdbcSelect = new JdbcOperationQuerySelect(
					sqlToUse( sql, executionContext ),
					jdbcParameterBinders,
					resultSetMapping,
					affectedTableNames
			);

			executionContext.getSession().autoFlushIfRequired( jdbcSelect.getAffectedTableNames() );
			return executionContext.getSession().getJdbcServices().getJdbcSelectExecutor().scroll(
					jdbcSelect,
					scrollMode,
					jdbcParameterBindings,
					SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext ),
					null,
					-1
			);
		}
	}
}
