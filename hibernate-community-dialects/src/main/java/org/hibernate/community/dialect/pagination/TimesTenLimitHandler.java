package org.hibernate.community.dialect.pagination;

import java.util.List;

import org.hibernate.dialect.pagination.spi.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code FIRST n} or {@code ROWS m TO n},
 * but at the start of the query, immediately after {@code SELECT}, instead of at the end.
 * <p>
 * The row numbers in {@code ROWS m TO n} are one-based and inclusive.
 */
public class TimesTenLimitHandler extends AbstractSimpleLimitHandler {

	public static final TimesTenLimitHandler INSTANCE = new TimesTenLimitHandler();

	public TimesTenLimitHandler(){
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	// TimesTen is 1 based
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult + 1;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	protected String insert(String limitClause, String sql) {
		return insertAfterSelect( limitClause, sql );
	}

	@Override
	protected List<Integer> bindLimitParameterValues(PaginationRequest request) {
		if ( request.hasMaxRows() ) {
			if ( request.hasFirstRow() ) {
				// rows (offset + 1) to (offset + max)
				final int firstRow = convertToFirstRowValue( request.firstRowOrZero() );
				final long lastRow = (long) request.firstRowOrZero() + request.maxRows();
				return List.of( firstRow, lastRow > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) lastRow );
			}
			// first max
			return List.of( request.maxRows() );
		}
		else if ( request.hasFirstRow() ) {
			return List.of( convertToFirstRowValue( request.firstRowOrZero() ) );
		}
		else {
			return List.of();
		}
	}

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		final String firstParameter = request.parameterMarker( 1 );
		if ( hasFirstRow ) {
			return " rows " + firstParameter + " to " + request.parameterMarker( 2 );
		}
		else {
			return " first " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " rows " + request.parameterMarker( 1 ) + " to " + Integer.MAX_VALUE;
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return request.hasMaxRows()
				? request.hasFirstRow() ? 3 : 2
				: request.hasFirstRow() ? 2 : 1;
	}
}
