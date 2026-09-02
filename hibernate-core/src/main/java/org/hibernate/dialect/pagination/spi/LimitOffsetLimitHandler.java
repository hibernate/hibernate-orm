/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * A {@link LimitHandler} for databases like PostgreSQL, H2,
 * and HSQL that support the syntax {@code LIMIT n OFFSET m}.
 * Note that this syntax does not allow specification of an
 * offset without a limit.
 *
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public class LimitOffsetLimitHandler extends AbstractSimpleLimitHandler {

	public static LimitOffsetLimitHandler INSTANCE = new LimitOffsetLimitHandler();
	public static LimitOffsetLimitHandler OFFSET_ONLY_INSTANCE = new LimitOffsetLimitHandler() {
		@Override
		protected String offsetOnlyClause(PaginationRequest request) {
			return " offset " + request.parameterMarker( request.jdbcParameterCount() + 1 );
		}
	};

	@SPI({ USE, IMPLEMENT })
	public LimitOffsetLimitHandler() {
	}

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		final String limit = " limit " + request.parameterMarker( request.jdbcParameterCount() + 1 );
		return hasFirstRow
				? limit + " offset " + request.parameterMarker( request.jdbcParameterCount() + 2 )
				: limit;
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " limit "
				+ Integer.MAX_VALUE
				+ " offset "
				+ request.parameterMarker( request.jdbcParameterCount() + 1 );
	}

	@Override
	public final boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

}
