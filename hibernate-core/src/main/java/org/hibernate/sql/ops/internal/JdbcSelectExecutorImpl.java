/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.internal;

import org.hibernate.sql.exec.internal.JdbcSelectExecutorStandardImpl;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcOperationQuerySelect;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.results.internal.ResultsHelper;
import org.hibernate.sql.results.internal.RowProcessingStateStandardImpl;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.hibernate.sql.results.jdbc.internal.JdbcValuesResultSetImpl;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.spi.ResultsConsumer;
import org.hibernate.sql.results.spi.RowReader;
import org.hibernate.sql.results.spi.RowTransformer;

import java.util.concurrent.TimeUnit;

/**
 * @author Steve Ebersole
 */
public class JdbcSelectExecutorImpl extends JdbcSelectExecutorStandardImpl {
	public static JdbcSelectExecutorImpl INSTANCE = new JdbcSelectExecutorImpl();

	public <T, R> T executeQuery(
			JdbcOperationQuerySelect jdbcSelect,
			JdbcParameterBindings jdbcParameterBindings,
			RowTransformer<R> rowTransformer,
			Class<R> domainResultType,
			int resultCountEstimate,
			JdbcValuesSourceProcessingStateCreator processingStateCreator,
			StatementCreator statementCreator,
			ResultsConsumer<T, R> resultsConsumer,
			ExecutionContext executionContext) {

		final var deferredResultSetAccess = new DeferredResultSetAccess(
				jdbcSelect,
				jdbcParameterBindings,
				executionContext,
				statementCreator,
				resultCountEstimate
		);
		final JdbcValues jdbcValues = resolveJdbcValuesSource(
				executionContext.getQueryIdentifier( deferredResultSetAccess.getFinalSql() ),
				jdbcSelect,
				resultsConsumer.canResultsBeCached(),
				executionContext,
				deferredResultSetAccess
		);

		if ( rowTransformer == null ) {
			rowTransformer = getRowTransformer( executionContext, jdbcValues );
		}

		final var session = executionContext.getSession();

		final boolean stats;
		long startTime = 0;
		final var statistics = session.getFactory().getStatistics();
		if ( executionContext.hasQueryExecutionToBeAddedToStatistics()
			&& jdbcValues instanceof JdbcValuesResultSetImpl ) {
			stats = statistics.isStatisticsEnabled();
			if ( stats ) {
				startTime = System.nanoTime();
			}
		}
		else {
			stats = false;
		}

		/*
		 * Processing options effectively are only used for entity loading.  Here we don't need these values.
		 */
		final JdbcValuesSourceProcessingOptions processingOptions = new JdbcValuesSourceProcessingOptions() {
			@Override
			public Object getEffectiveOptionalObject() {
				return executionContext.getEntityInstance();
			}

			@Override
			public String getEffectiveOptionalEntityName() {
				return null;
			}

			@Override
			public Object getEffectiveOptionalId() {
				return executionContext.getEntityId();
			}

			@Override
			public boolean shouldReturnProxies() {
				return true;
			}
		};

		final var valuesProcessingState = processingStateCreator.createState(
				executionContext,
				processingOptions
		);

		final RowReader<R> rowReader = ResultsHelper.createRowReader(
				session.getFactory(),
				rowTransformer,
				domainResultType,
				jdbcValues
		);

		final var rowProcessingState = new RowProcessingStateStandardImpl(
				valuesProcessingState,
				executionContext,
				rowReader,
				jdbcValues
		);

		final T result = resultsConsumer.consume(
				jdbcValues,
				session,
				processingOptions,
				valuesProcessingState,
				rowProcessingState,
				rowReader
		);

		if ( stats ) {
			final long endTime = System.nanoTime();
			final long milliseconds =
					TimeUnit.MILLISECONDS.convert( endTime - startTime, TimeUnit.NANOSECONDS );
			statistics.queryExecuted(
					executionContext.getQueryIdentifier( jdbcSelect.getSqlString() ),
					getResultSize( result ),
					milliseconds
			);
		}

		return result;
	}
}
