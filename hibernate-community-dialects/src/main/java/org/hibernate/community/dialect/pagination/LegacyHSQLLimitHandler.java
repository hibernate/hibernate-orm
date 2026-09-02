/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.spi.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

/**
 * A {@link LimitHandler} for HSQL prior to 2.0.
 */
public class LegacyHSQLLimitHandler extends AbstractSimpleLimitHandler {

	public static LegacyHSQLLimitHandler INSTANCE = new LegacyHSQLLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		if ( request.usesStandardParameterMarkers() ) {
			return hasFirstRow ? " limit ? ?" : " top ?";
		}
		final String firstParameter = request.parameterMarker( 1 );
		if ( hasFirstRow ) {
			return " limit 1+" + firstParameter + " " + request.parameterMarker( 2 );
		}
		else {
			return " top " + firstParameter;
		}
	}

	@Override
	protected String insert(String limitOrTop, String sql) {
		return insertAfterSelect( limitOrTop, sql );
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return request.hasMaxRows()
				? request.hasFirstRow() ? 3 : 2
				: request.hasFirstRow() ? 2 : 1;
	}
}
