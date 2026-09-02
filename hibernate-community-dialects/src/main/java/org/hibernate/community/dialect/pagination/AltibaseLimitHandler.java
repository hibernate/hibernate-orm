/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

/**
 * Limit handler for {@link org.hibernate.community.dialect.AltibaseDialect}.
 *
 * @author Geoffrey park
 */
public class AltibaseLimitHandler extends LimitLimitHandler {
	public static final AltibaseLimitHandler INSTANCE = new AltibaseLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		final String firstParameter = request.parameterMarker( request.jdbcParameterCount() + 1 );
		if ( hasFirstRow ) {
			return " limit 1+" + firstParameter + "," + request.parameterMarker( request.jdbcParameterCount() + 2 );
		}
		else {
			return " limit " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " limit 1+" + request.parameterMarker( request.jdbcParameterCount() + 1 ) + "," + Integer.MAX_VALUE;
	}
}
