/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.AbstractNoOffsetLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for older versions of Informix, Ingres,
 * and TimesTen, which supported the syntax {@code SELECT FIRST n}.
 * Note that this syntax does not allow specification of an offset.
 *
 * @author Chris Cranford
 */
public class FirstLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FirstLimitHandler INSTANCE = new FirstLimitHandler(false);

	public FirstLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " first ?";
	}

	@Override
	protected String limitClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " first " + parameterMarkerStrategy.createMarker( 1, null ) + " rows only";
	}

	@Override
	protected String insert(String first, String sql) {
		return insertAfterSelect( first, sql );
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
