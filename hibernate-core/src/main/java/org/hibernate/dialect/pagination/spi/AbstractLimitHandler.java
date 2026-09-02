/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.hibernate.SPI;
import org.hibernate.query.Query;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Default implementation of {@link LimitHandler} interface.
 *
 * @author Lukasz Antoniak
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractLimitHandler implements LimitHandler {
	@SPI(IMPLEMENT)
	protected AbstractLimitHandler() {
	}

	public static final LimitHandler NO_LIMIT = request -> PaginationResult.unchanged( request.sql() );

	private static final Pattern SELECT_PATTERN =
			compile( "^\\s*select\\b", CASE_INSENSITIVE );

	private static final Pattern SELECT_DISTINCT_PATTERN =
			compile( "^\\s*select(\\s+(distinct|all))?\\b", CASE_INSENSITIVE );

	private static final Pattern END_PATTERN =
			compile("\\s*;?\\s*$", CASE_INSENSITIVE);

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s*;?\\s*$", CASE_INSENSITIVE);


	@Override
	public boolean supportsLimit() {
		return false;
	}

//	@Override
	public boolean supportsOffset() {
		return false;
	}

	@Override
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	/**
	 * Does this handler support bind variables (JDBC prepared statement
	 * parameters) for its limit/offset?
	 *
	 * @return true if bind variables can be used
	 */
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}

	/**
	 * Usually, the offset comes before the limit, but occasionally the
	 * offset is specified after the limit. Does this dialect require us
	 * to bind the parameters in reverse order?
	 *
	 * @return true if the correct order is limit then offset
	 */
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	/**
	 * Does the offset/limit clause come at the start of the
	 * {@code SELECT} statement, or at the end of the query?
	 *
	 * @return true if limit parameters come before other parameters
	 */
	public boolean bindLimitParametersFirst() {
		return false;
	}

	/**
	 * Does the limit clause expect the number of the last row, or the
	 * "page size", the maximum number of rows we want to receive?
	 * Hibernate's {@link Query#setMaxResults(int)}
	 * accepts the page size, so the number of the last row is obtained
	 * by adding the number of the first row, which is one greater than
	 * {@link Query#setFirstResult(int)}.
	 *
	 * @return true if the limit clause expects the number of
	 *         the last row, false if it expects the page size
	 */
	public boolean useMaxForLimit() {
		//if this limit handler doesn't support
		//an offset, we definitely need to set
		//the limit to the last row
		return !supportsLimitOffset();
	}

	/**
	 * Generally, if there is no limit applied to a Hibernate query we do not
	 * apply any limits to the SQL query. This option forces that the limit
	 * be written to the SQL query.
	 *
	 * @return true to force limit into SQL query even if none specified in
	 *         Hibernate query; false otherwise.
	 */
	public boolean forceLimitUsage() {
		return false;
	}

	/**
	 * The API method {@link Query#setFirstResult(int)} accepts a zero-based offset.
	 * Does this dialect require a one-based offset to be specified in the offset clause?
	 *
	 * @implNote The value passed into {@link AbstractLimitHandler#processSql(PaginationRequest)}
	 *           has a zero-based offset. Handlers which do not {@link #supportsVariableLimit}
	 *           should take care to perform any needed first-row-conversion calls prior to
	 *           injecting the limit values into the SQL string.
	 *
	 * @param zeroBasedFirstResult The user-supplied, zero-based first row offset.
	 *
	 * @return The resulting offset, adjusted to one-based if necessary.
	 */
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}


	@Override
	public PaginationResult processSql(PaginationRequest request) {
		throw new UnsupportedOperationException( "Paged queries not supported by " + getClass().getName() );
	}

	protected final PaginationResult result(PaginationRequest request, String sql) {
		return new PaginationResult( sql, jdbcInstructions( request ) );
	}

	protected PaginationJdbcInstructions jdbcInstructions(PaginationRequest request) {
		final List<Integer> values = bindLimitParameterValues( request );
		final List<Integer> parametersAtStart = bindLimitParametersFirst() ? values : List.of();
		final List<Integer> parametersAtEnd = bindLimitParametersFirst() ? List.of() : values;
		return new PaginationJdbcInstructions(
				parametersAtStart,
				parametersAtEnd,
				jdbcMaxRows( request ),
				rowsToSkip( request )
		);
	}

	protected Integer jdbcMaxRows(PaginationRequest request) {
		return null;
	}

	protected int rowsToSkip(PaginationRequest request) {
		return request.hasFirstRow() && !supportsLimitOffset() ? request.firstRowOrZero() : 0;
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return jdbcInstructions( request ).parameterPositionStart();
	}

	protected List<Integer> bindLimitParameterValues(PaginationRequest request) {
		if ( supportsVariableLimit() ) {
			final boolean hasMaxRows = request.hasMaxRows();
			final boolean hasFirstRow = request.hasFirstRow();
			final boolean bindLimit
					= hasMaxRows && supportsLimit()
					|| forceLimitUsage();
			final boolean bindOffset
					= hasFirstRow && supportsOffset()
					|| hasFirstRow && hasMaxRows && supportsLimitOffset();
			return bindLimitParameterValues( request, bindOffset, bindLimit );
		}
		else {
			return List.of();
		}
	}

	private List<Integer> bindLimitParameterValues(
			PaginationRequest request,
			boolean bindOffset, boolean bindLimit)
	{
		final List<Integer> values = new ArrayList<>( 2 );
		if ( bindLimitParametersInReverseOrder() ) {
			addLimit( request, bindLimit, values );
			addOffset( request, bindOffset, values );
		}
		else {
			addOffset( request, bindOffset, values );
			addLimit( request, bindLimit, values );
		}
		return List.copyOf( values );
	}

	private void addOffset(PaginationRequest request, boolean bindOffset, List<Integer> values) {
		if ( bindOffset ) {
			values.add( getFirstRow( request ) );
		}
	}

	private void addLimit(PaginationRequest request, boolean bindLimit, List<Integer> values) {
		if ( bindLimit ) {
			values.add( getMaxOrLimit( request ) );
		}
	}

	/**
	 * Is a max row limit indicated?
	 *
	 * @param request the immutable pagination request
	 *
	 * @return Whether a max row limit was indicated
	 */
	public static boolean hasMaxRows(PaginationRequest request) {
		return request != null && request.hasMaxRows();
	}

	/**
	 * Is a first row limit indicated?
	 *
	 * @param request the immutable pagination request
	 *
	 * @return Whether a first row limit was indicated
	 */
	public static boolean hasFirstRow(PaginationRequest request) {
		return request != null && request.hasFirstRow();
	}

	/**
	 * Some dialect-specific LIMIT clauses require the maximum last row number
	 * (aka, first_row_number + total_row_count), while others require the maximum
	 * returned row count (the total maximum number of rows to return).
	 *
	 * @param request the immutable pagination request
	 *
	 * @return The appropriate value to bind into the limit clause.
	 */
	protected final int getMaxOrLimit(PaginationRequest request) {
		if ( request.hasMaxRows() ) {
			final int firstRow = getFirstRow( request );
			final int maxRows = request.maxRows();
			final int maxOrLimit =
					useMaxForLimit()
							? maxRows + firstRow //TODO: maxRows + firstRow - 1, surely?
							: maxRows;
			// Use Integer.MAX_VALUE on overflow
			return maxOrLimit < 0 ? Integer.MAX_VALUE : maxOrLimit;
		}
		else {
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Retrieve the indicated first row for pagination
	 *
	 * @param request the immutable pagination request
	 *
	 * @return The first row
	 */
	protected final int getFirstRow(PaginationRequest request) {
		return convertToFirstRowValue( request.firstRowOrZero() );
	}

	/**
	 * Insert a fragment of SQL right after
	 * {@code SELECT}, but before {@code DISTINCT}
	 * or {@code ALL}.
	 */
	protected static String insertAfterSelect(String limitOffsetClause, String sqlStatement) {
		final var selectMatcher = SELECT_PATTERN.matcher( sqlStatement );
		if ( selectMatcher.find() ) {
			return new StringBuilder( sqlStatement )
					.insert( selectMatcher.end(), limitOffsetClause )
					.toString();
		}
		else {
			return sqlStatement;
		}
	}

	/**
	 * Insert a fragment of SQL right after
	 * {@code SELECT}, {@code SELECT DISTINCT},
	 * or {@code SELECT ALL}.
	 */
	protected static String insertAfterDistinct(String limitOffsetClause, String sqlStatement) {
		final var selectDistinctMatcher = SELECT_DISTINCT_PATTERN.matcher( sqlStatement );
		if ( selectDistinctMatcher.find() ) {
			return new StringBuilder( sqlStatement )
					.insert( selectDistinctMatcher.end(), limitOffsetClause )
					.toString();
		}
		else {
			return sqlStatement;
		}
	}

	/**
	 * Insert a fragment of SQL right at the very
	 * end of the query.
	 */
	protected String insertAtEnd(String limitOffsetClause, String sqlStatement) {
		final var endMatcher = END_PATTERN.matcher( sqlStatement );
		if ( endMatcher.find() ) {
			return new StringBuilder( sqlStatement )
					.insert( endMatcher.start(), limitOffsetClause )
					.toString();
		}
		else {
			return sqlStatement;
		}
	}

	/**
	 * The offset/limit clauses typically must come
	 * before the {@code FOR UPDATE}ish clauses, so
	 * we need a way to identify these clauses in
	 * the text of the whole query.
	 */
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}

	/**
	 * Insert a fragment of SQL right before the
	 * {@code FOR UPDATE}ish clauses at the end
	 * of the query.
	 */
	protected String insertBeforeForUpdate(String limitOffsetClause, String sqlStatement) {
		final var forUpdateMatcher = getForUpdatePattern().matcher( sqlStatement );
		if ( forUpdateMatcher.find() ) {
			return new StringBuilder( sqlStatement )
					.insert( forUpdateMatcher.start(), limitOffsetClause )
					.toString();
		}
		else {
			return sqlStatement;
		}
	}

}
