package org.hibernate.community.dialect;

import java.util.List;

import org.hibernate.community.dialect.pagination.TimesTenLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationJdbcInstructions;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the SQL and the bind values produced by {@link TimesTenLimitHandler}
 * without requiring a database.
 * <p>
 * TimesTen row numbers are one-based and inclusive, and the limit clause
 * is placed immediately after {@code select}.
 */
public class TimesTenLimitHandlerTest {

	private static final String SQL = "select b.id from Book b order by b.id";

	private final TimesTenLimitHandler handler = TimesTenLimitHandler.INSTANCE;

	@Test
	void maxRowsOnlyUsesFirst() {
		final PaginationResult result = handler.processSql( request( null, 5 ) );

		assertThat( result.sql() ).isEqualTo( "select first ? b.id from Book b order by b.id" );
		assertThat( startParameters( result ) ).containsExactly( 5 );
	}

	@Test
	void firstRowAndMaxRowsUseInclusiveOneBasedRowNumbers() {
		final PaginationResult result = handler.processSql( request( 5, 5 ) );

		assertThat( result.sql() ).isEqualTo( "select rows ? to ? b.id from Book b order by b.id" );
		assertThat( startParameters( result ) ).containsExactly( 6, 10 );
	}

	@Test
	void zeroFirstRowWithMaxRowsStillUsesRowsTo() {
		final PaginationResult result = handler.processSql( request( 0, 5 ) );

		assertThat( result.sql() ).isEqualTo( "select rows ? to ? b.id from Book b order by b.id" );
		assertThat( startParameters( result ) ).containsExactly( 1, 5 );
	}

	@Test
	void lastRowIsClampedWhenOffsetPlusMaxRowsOverflows() {
		final PaginationResult result = handler.processSql( request( Integer.MAX_VALUE - 1, 10 ) );

		assertThat( startParameters( result ) ).containsExactly( Integer.MAX_VALUE, Integer.MAX_VALUE );
	}

	@Test
	void firstRowOnlyRunsToTheLastRow() {
		final PaginationResult result = handler.processSql( request( 10, null ) );

		assertThat( result.sql() )
				.isEqualTo( "select rows ? to " + Integer.MAX_VALUE + " b.id from Book b order by b.id" );
		assertThat( startParameters( result ) ).containsExactly( 11 );
	}

	@Test
	void noPaginationLeavesSqlUnchanged() {
		final PaginationResult result = handler.processSql( request( null, null ) );

		assertThat( result.sql() ).isEqualTo( SQL );
		assertThat( result.jdbcInstructions() ).isEqualTo( PaginationJdbcInstructions.NONE );
	}

	@Test
	void parametersAreBoundBeforeTheQueryParameters() {
		assertThat( handler.bindLimitParametersFirst() ).isTrue();
		assertThat( handler.parameterPositionStart( request( null, 5 ) ) ).isEqualTo( 2 );
		assertThat( handler.parameterPositionStart( request( 5, 5 ) ) ).isEqualTo( 3 );
		assertThat( handler.parameterPositionStart( request( 5, null ) ) ).isEqualTo( 2 );
		assertThat( handler.parameterPositionStart( request( null, null ) ) ).isEqualTo( 1 );
	}

	private static PaginationRequest request(Integer firstRow, Integer maxRows) {
		return new PaginationRequest( SQL, firstRow, maxRows, 0, null );
	}

	private static List<Integer> startParameters(PaginationResult result) {
		return result.jdbcInstructions().parametersAtStart();
	}
}
