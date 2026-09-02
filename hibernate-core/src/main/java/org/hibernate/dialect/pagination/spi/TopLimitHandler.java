/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;


/**
 * A {@link LimitHandler} for Transact SQL and similar
 * databases which support the syntax {@code SELECT TOP n}.
 * Note that this syntax does not allow specification of
 * an offset.
 *
 * @author Brett Meyer
 * @since 8.0
 */
@SPI
public final class TopLimitHandler extends AbstractNoOffsetLimitHandler {

	public static TopLimitHandler INSTANCE = new TopLimitHandler(true);

	@SPI
	public TopLimitHandler(boolean variableLimit) {
		super(variableLimit);
	}

	@Override
	protected String limitClause(PaginationRequest request) {
		return request.usesStandardParameterMarkers()
				? " top ? "
				: " top " + request.parameterMarker( 1 ) + " rows only";
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
	public int parameterPositionStart(PaginationRequest request) {
		return request.hasMaxRows() && supportsVariableLimit() ? 2 : 1;
	}
}
