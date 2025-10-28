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
 * A {@link LimitHandler} for HSQL prior to 2.0.
 */
public class LegacyHSQLLimitHandler extends AbstractSimpleLimitHandler {

	public static LegacyHSQLLimitHandler INSTANCE = new LegacyHSQLLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ? ?" : " top ?";
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String firstParameter = parameterMarkerStrategy.createMarker( 1, null );
		if ( hasFirstRow ) {
			return " limit 1+" + firstParameter + " " + parameterMarkerStrategy.createMarker( 2, null );
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
	public boolean processSqlMutatesState() {
		return false;
	}

	@Override
	public int getParameterPositionStart(Limit limit) {
		return hasMaxRows( limit )
				? hasFirstRow( limit ) ? 3 : 2
				: hasFirstRow( limit ) ? 2 : 1;
	}
}
