/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for TimesTen, which uses {@code ROWS n},
 * but at the start of the query instead of at the end.
 */
public class TimesTenLimitHandler extends RowsLimitHandler {

	public static final TimesTenLimitHandler INSTANCE = new TimesTenLimitHandler();

	@Override
	protected String insert(String rows, String sql) {
		return insertAfterSelect( rows, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String firstParameter = parameterMarkerStrategy.createMarker( 1, null );
		if ( hasFirstRow ) {
			return " rows " + firstParameter + " to " + parameterMarkerStrategy.createMarker( 2, null );
		}
		else {
			return " rows " + firstParameter;
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
