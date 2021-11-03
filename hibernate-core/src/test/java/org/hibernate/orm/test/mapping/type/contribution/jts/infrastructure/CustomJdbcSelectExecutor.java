/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts.infrastructure;

import java.util.List;
import java.util.stream.Stream;

import org.hibernate.ScrollMode;
import org.hibernate.query.spi.ScrollableResultsImplementor;
import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.exec.spi.JdbcSelectExecutor;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.sql.results.spi.RowTransformer;

/**
 * @author Steve Ebersole
 */
public class CustomJdbcSelectExecutor implements JdbcSelectExecutor {
	private final JdbcSelectExecutorStandardImpl standardSelectExecutor;

	public CustomJdbcSelectExecutor() {
		this.standardSelectExecutor = JdbcSelectExecutorStandardImpl.INSTANCE;
	}

	@Override
	public <R> List<R> list(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer,
			ListResultsConsumer.UniqueSemantic uniqueSemantic) {
		return standardSelectExecutor.list(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				rowTransformer,
				uniqueSemantic,
				(preparedStatementFunction) -> new CustomResultSetAccess(
						jdbcSelect,
						jdbcParameterBindings,
						executionContext,
						preparedStatementFunction
				)
		);
	}

	@Override
	public <R> ScrollableResultsImplementor<R> scroll
			(JdbcSelect jdbcSelect,
			 ScrollMode scrollMode,
			 JdbcParameterBindings jdbcParameterBindings,
			 ExecutionContext executionContext,
			 RowTransformer<R> rowTransformer) {
		return standardSelectExecutor.scroll(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(preparedStatementFunction) -> new CustomResultSetAccess(
						jdbcSelect,
						jdbcParameterBindings,
						executionContext,
						preparedStatementFunction
				)
		);
	}

	@Override
	public <R> Stream<R> stream(
			JdbcSelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			ExecutionContext executionContext,
			RowTransformer<R> rowTransformer) {
		return standardSelectExecutor.stream(
				jdbcSelect,
				executionContext,
				rowTransformer,
				(preparedStatementFunction) -> new CustomResultSetAccess(
						jdbcSelect,
						jdbcParameterBindings,
						executionContext,
						preparedStatementFunction
				)
		);
	}
}
