/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.query.sql.spi.NativeSelectQueryPlan;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.internal.JdbcSelectImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.exec.spi.RowTransformer;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * @author Steve Ebersole
 */
public class NativeSelectQueryPlanImpl<R> implements NativeSelectQueryPlan<R> {
	private final String sql;
	private final Set<String> affectedTableNames;

	private final List<JdbcParameterBinder> parameterBinders;

	private final ResultSetMappingDescriptor resultSetMapping;
	private final RowTransformer<R> rowTransformer;

	public NativeSelectQueryPlanImpl(
			String sql,
			Set<String> affectedTableNames,
			List<JdbcParameterBinder> parameterBinders,
			ResultSetMappingDescriptor resultSetMapping,
			RowTransformer<R> rowTransformer) {
		this.sql = sql;
		this.affectedTableNames = affectedTableNames;
		this.parameterBinders = parameterBinders;
		this.resultSetMapping = resultSetMapping;
		this.rowTransformer = rowTransformer;
	}

	@Override
	public List<R> performList(ExecutionContext executionContext) {
		// todo (6.0) : per email to dev group I'd like to remove this `callable` support
		//		so for now, ignore it and simply build the JdbcSelect
		final JdbcSelect jdbcSelect = new JdbcSelectImpl(
				sql,
				parameterBinders,
				resultSetMapping,
				affectedTableNames
		);

		// todo (6.0) : need to make this swappable (see note in executor class)
		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.list( jdbcSelect, executionContext, rowTransformer );
	}

	@Override
	public ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext) {
		// todo (6.0) : see notes above in `#performList`
		final JdbcSelect jdbcSelect = new JdbcSelectImpl(
				sql,
				parameterBinders,
				resultSetMapping,
				affectedTableNames
		);
		final JdbcSelectExecutor executor = JdbcSelectExecutorStandardImpl.INSTANCE;

		return executor.scroll( jdbcSelect, scrollMode, executionContext, rowTransformer );
	}
}
