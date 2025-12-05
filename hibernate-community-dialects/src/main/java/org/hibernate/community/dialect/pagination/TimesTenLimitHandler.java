/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

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
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " rows ? to ?" : " first ?";
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String firstParameter = parameterMarkerStrategy.createMarker( 1, null );
		if ( hasFirstRow ) {
			return " rows " + firstParameter + " to " + parameterMarkerStrategy.createMarker( 2, null );
		}
		else {
			return " first " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " rows " + parameterMarkerStrategy.createMarker( 1, null ) + " to " + Integer.MAX_VALUE;
	}

	@Override
	public int getParameterPositionStart(Limit limit) {
		return hasMaxRows( limit )
				? hasFirstRow( limit ) ? 3 : 2
				: hasFirstRow( limit ) ? 2 : 1;
	}
}
