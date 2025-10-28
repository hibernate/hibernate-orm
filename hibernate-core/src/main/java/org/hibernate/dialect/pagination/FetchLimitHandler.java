/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

/**
 * A {@link LimitHandler} for databases which support the ANSI
 * SQL standard syntax {@code FETCH FIRST m ROWS ONLY} but not
 * {@code OFFSET n ROWS}.
 *
 * @author Gavin King
 */
public class FetchLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FetchLimitHandler INSTANCE = new FetchLimitHandler(false);

	public FetchLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause() {
		return " fetch first ? rows only";
	}

	@Override
	protected String limitClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " fetch first " + parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null ) + " rows only";
	}

	@Override
	protected String insert(String fetch, String sql) {
		return insertBeforeForUpdate( fetch, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}

}
