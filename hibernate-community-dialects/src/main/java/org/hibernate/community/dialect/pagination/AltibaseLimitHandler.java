/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.LimitLimitHandler;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * Limit handler for {@link org.hibernate.community.dialect.AltibaseDialect}.
 *
 * @author Geoffrey park
 */
public class AltibaseLimitHandler extends LimitLimitHandler {
	public static final AltibaseLimitHandler INSTANCE = new AltibaseLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit 1+?,?" : " limit ?";
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String firstParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
		if ( hasFirstRow ) {
			return " limit 1+" + firstParameter + "," + parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null );
		}
		else {
			return " limit " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit 1+?," + Integer.MAX_VALUE;
	}

	@Override
	protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " limit 1+" + parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null ) + "," + Integer.MAX_VALUE;
	}
}
