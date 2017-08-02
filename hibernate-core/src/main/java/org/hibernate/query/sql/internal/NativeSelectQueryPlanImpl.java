/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.sql.exec.results.spi.ResultSetMapping;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.JdbcSelectImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class NativeSelectQueryPlanImpl<R> implements NativeSelectQueryPlan<R> {
	private final String sql;
	private final boolean callable;

	private final List<JdbcParameterBinder> parameterBinders;

	private final ResultSetMapping resultSetMapping;
	private final RowTransformer<R> rowTransformer;

	public NativeSelectQueryPlanImpl(
			String sql,
			boolean callable,
			List<JdbcParameterBinder> parameterBinders,
			ResultSetMapping resultSetMapping,
			RowTransformer<R> rowTransformer) {
		this.sql = sql;
		this.callable = callable;
		this.parameterBinders = parameterBinders;
		this.resultSetMapping = resultSetMapping;
		this.rowTransformer = rowTransformer;
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		// todo (6.0) : per email to dev group I'd like to remove this `callable` support
		//		so for now, ignore it and simply build the JdbcSelect
		final JdbcSelect jdbcSelect = new JdbcSelectImpl( sql, parameterBinders, resultSetMapping );

		// todo (6.0) : need to make this swappable (see not in executor class)
		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.list( jdbcSelect, executionContext, rowTransformer );
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		// todo (6.0) : see notes above in `#performList`
		final JdbcSelect jdbcSelect = new JdbcSelectImpl( sql, parameterBinders, resultSetMapping );
		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.scroll( jdbcSelect, scrollMode, executionContext, rowTransformer );
	}
}
