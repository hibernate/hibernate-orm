/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.spi.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code ROWS n},
 * but at the start of the query instead of at the end.
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
