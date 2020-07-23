/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.ScrollMode;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.internal.RowTransformerPassThruImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;
import org.hibernate.sql.results.spi.RowTransformer;

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
			JdbcValuesMappingProducer resultSetMapping,
			RowTransformer<R> rowTransformer) {
		this.sql = sql;
		this.affectedTableNames = affectedTableNames;
		this.parameterList = parameterList;
		this.resultSetMapping = resultSetMapping;
		this.rowTransformer = rowTransformer != null
				? rowTransformer
				: RowTransformerPassThruImpl.instance();
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		final List<JdbcParameterBinder> jdbcParameterBinders = resolveJdbcParamBinders( executionContext );
		final JdbcParameterBindings jdbcParameterBindings = resolveJdbcParamBindings( executionContext, jdbcParameterBinders );

		final JdbcSelect jdbcSelect = new JdbcSelect(
				sql,
				jdbcParameterBinders,
				resultSetMapping,
				affectedTableNames,
				Collections.emptySet()
		);

		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.list(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				false
		);
	}

	private List<JdbcParameterBinder> resolveJdbcParamBinders(ExecutionContext executionContext) {
		if ( parameterList == null || parameterList.isEmpty() ) {
			return Collections.emptyList();
		}

		throw new NotYetImplementedFor6Exception( getClass() );
	}

	private JdbcParameterBindings resolveJdbcParamBindings(
			ExecutionContext executionContext,
			List<JdbcParameterBinder> jdbcParameterBinders) {
		if ( jdbcParameterBinders.isEmpty() ) {
			return JdbcParameterBindings.NO_BINDINGS;
		}

		throw new NotYetImplementedFor6Exception( getClass() );
	}

//	private List<JdbcParameterBinder> resolveJdbcParameterBinders(ExecutionContext executionContext) {
//		final List<JdbcParameterBinder> jdbcParameterBinders = CollectionHelper.arrayList( parameterList.size() );
//
//		for ( QueryParameterImplementor parameter : parameterList ) {
//			final QueryParameterBinding parameterBinding = executionContext.getDomainParameterBindingContext()
//					.getQueryParameterBindings()
//					.getBinding( parameter );
//			AllowableParameterType type = parameterBinding.getBindType();
//			if ( type == null ) {
//				type = parameter.getHibernateType();
//			}
//
//			type.dehydrate(
//					type.unresolve( parameterBinding.getBindValue(), executionContext.getSession() ),
//					(jdbcValue, sqlExpressableType, boundColumn) -> jdbcParameterBinders.add(
//							(statement, startPosition, jdbcParameterBindings, executionContext1) -> {
//								//noinspection unchecked
//								sqlExpressableType.getJdbcValueBinder().bind(
//										statement,
//										startPosition,
//										jdbcValue,
//										executionContext1
//								);
//								return 1;
//							}
//					),
//					Clause.IRRELEVANT,
//					executionContext.getSession()
//			);
//		}
//
//		return jdbcParameterBinders;
//	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		final List<JdbcParameterBinder> jdbcParameterBinders = resolveJdbcParamBinders( executionContext );
		final JdbcParameterBindings jdbcParameterBindings = resolveJdbcParamBindings( executionContext, jdbcParameterBinders );

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
