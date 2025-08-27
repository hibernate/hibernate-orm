/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for databases like PostgreSQL, H2,
 * and HSQL that support the syntax {@code LIMIT n OFFSET m}.
 * Note that this syntax does not allow specification of an
 * offset without a limit.
 */
public class LimitOffsetLimitHandler extends AbstractSimpleLimitHandler {

	public static LimitOffsetLimitHandler INSTANCE = new LimitOffsetLimitHandler();
	public static LimitOffsetLimitHandler OFFSET_ONLY_INSTANCE = new LimitOffsetLimitHandler() {
		@Override
		protected String offsetOnlyClause() {
			return " offset ?";
		}
		@Override
		protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
			return " offset " + parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
		}
	};

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ? offset ?" : " limit ?";
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String firstParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
		if ( hasFirstRow ) {
			return " limit " + firstParameter + " offset " + parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null );
		}
		else {
			return " limit " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit " + Integer.MAX_VALUE + " offset ?";
	}

	@Override
	protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " limit " + Integer.MAX_VALUE + " offset " + parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
	}

	@Override
	public final boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}
}
