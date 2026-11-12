package org.hibernate.sql.results.spi;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.jdbc.spi.JdbcValues;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingOptions;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Consumes {@link JdbcValues} and returns the consumed values in whatever form this
 * consumer returns, generally a {@link java.util.List} or a {@link org.hibernate.ScrollableResults}
 *
 * @see org.hibernate.sql.exec.spi.JdbcSelectExecutor#executeQuery(org.hibernate.sql.exec.spi.JdbcSelect, org.hibernate.sql.exec.spi.JdbcParameterBindings, org.hibernate.sql.exec.spi.ExecutionContext, RowTransformer, Class, org.hibernate.sql.exec.spi.StatementCreator, ResultsConsumer)
 * @see org.hibernate.query.spi.SelectQueryPlan#executeQuery(org.hibernate.query.spi.DomainQueryExecutionContext, ResultsConsumer)
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultsConsumer<T, R> {
	/// Consumes one JDBC result stream using only supported processing contexts.
	/// Materializing consumers must complete row processing before returning and
	/// must not retain row-processing state for deferred result finalization.
	/// Cursor consumers may transfer processing state to the returned cursor,
	/// which owns that state until it is closed.
	T consume(
			JdbcValues jdbcValues,
			SharedSessionContractImplementor session,
			JdbcValuesSourceProcessingOptions processingOptions,
			JdbcValuesSourceProcessingState jdbcValuesSourceProcessingState,
			RowProcessingState rowProcessingState,
			RowReader<R> rowReader);

	boolean canResultsBeCached();

	/// Whether this consumer can finish its result after successful locking post-actions.
	/// For affected direct force-increment executions, participating consumers receive
	/// query options without a result-list transformer during consumption;
	/// [#finishResult] receives the original options. The default is `false`,
	/// preserving the existing consumption timing of custom consumers.
	default boolean supportsDeferredResultFinalization() {
		return false;
	}

	/// Finalizes an affected direct force-increment result once its supplemental locks
	/// and increments have completed, using the original execution context and query
	/// options. Participating materializing consumers must finish row processing in
	/// `consume()` and perform any deferred result-list transformation only here.
	/// This hook is not called for ordinary, non-deferred executions.
	default T finishResult(T result, ExecutionContext executionContext) {
		return result;
	}
}
