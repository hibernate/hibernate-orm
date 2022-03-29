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
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.EmptyScrollableResults;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.spi.DomainQueryExecutionContext;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.query.sql.spi.ParameterOccurrence;
import org.hibernate.query.sqm.internal.SqmJdbcExecutionContextAdapter;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/**
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
	public List<R> performList(DomainQueryExecutionContext executionContext) {
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

			jdbcParameterBindings.registerNativeQueryParameters(
					queryParameterBindings,
					parameterList,
					jdbcParameterBinders,
					executionContext.getSession().getFactory()
			);
		}

		executionContext.getSession().autoFlushIfRequired( affectedTableNames );

		final JdbcSelect jdbcSelect = new JdbcSelect(
				sql,
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames,
				Collections.emptySet()
		);

		final SharedSessionContractImplementor session = executionContext.getSession();
		final SessionFactoryImplementor factory = session.getFactory();
		final JdbcServices jdbcServices = factory.getJdbcServices();
		return jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext ),
				null,
				ListResultsConsumer.UniqueSemantic.NEVER
		);
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext) {
		if ( executionContext.getQueryOptions().getEffectiveLimit().getMaxRowsJpa() == 0 ) {
			return EmptyScrollableResults.INSTANCE;
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

			jdbcParameterBindings.registerNativeQueryParameters(
					queryParameterBindings,
					parameterList,
					jdbcParameterBinders,
					executionContext.getSession().getFactory()
			);
		}

		final JdbcSelect jdbcSelect = new JdbcSelect(
				sql,
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames,
				Collections.emptySet()
		);

		return executionContext.getSession().getJdbcServices().getJdbcSelectExecutor().scroll(
				jdbcSelect,
				scrollMode,
				jdbcParameterBindings,
				SqmJdbcExecutionContextAdapter.usingLockingAndPaging( executionContext ),
				null
		);
	}
}
