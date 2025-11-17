/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.query.spi.Limit;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for Transact SQL and similar
 * databases which support the syntax {@code SELECT TOP n}.
 * Note that this syntax does not allow specification of
 * an offset.
 *
 * @author Brett Meyer
 */
public class TopLimitHandler extends AbstractNoOffsetLimitHandler {

	public static TopLimitHandler INSTANCE = new TopLimitHandler(true);

	public TopLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " top ? ";
	}

	@Override
	protected String limitClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " top " + parameterMarkerStrategy.createMarker( 1, null ) + " rows only";
	}

	@Override
	protected String insert(String limitClause, String sql) {
		return insertAfterDistinct( limitClause, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}

	@Override
	public int getParameterPositionStart(Limit limit) {
		return hasMaxRows( limit ) && supportsVariableLimit() ? 2 : 1;
	}
}
