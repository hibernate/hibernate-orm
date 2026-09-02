/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;


/**
 * A {@link LimitHandler} for databases which support the ANSI
 * SQL standard syntax {@code FETCH FIRST m ROWS ONLY} but not
 * {@code OFFSET n ROWS}.
 *
 * @author Gavin King
 * @since 8.0
 */
@SPI
public final class FetchLimitHandler extends AbstractNoOffsetLimitHandler {

	public static final FetchLimitHandler INSTANCE = new FetchLimitHandler(false);

	@SPI
	public FetchLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause(PaginationRequest request) {
		return " fetch first "
				+ request.parameterMarker( request.jdbcParameterCount() + 1 )
				+ " rows only";
	}

	@Override
	protected String insert(String fetch, String sql) {
		return insertBeforeForUpdate( fetch, sql );
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

}
