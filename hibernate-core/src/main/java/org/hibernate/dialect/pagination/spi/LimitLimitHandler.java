/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;


import java.util.regex.Pattern;

import org.hibernate.SPI;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Limit handler for MySQL and CUBRID which support the syntax
 * {@code LIMIT n} and {@code LIMIT m, n}. Note that this
 * syntax does not allow specification of an offset without
 * a limit.
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public class LimitLimitHandler extends AbstractSimpleLimitHandler {

	public static final LimitLimitHandler INSTANCE = new LimitLimitHandler();

	@SPI({ USE, IMPLEMENT })
	public LimitLimitHandler() {
	}

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		final String limit = " limit " + request.parameterMarker( request.jdbcParameterCount() + 1 );
		return hasFirstRow
				? limit + "," + request.parameterMarker( request.jdbcParameterCount() + 2 )
				: limit;
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " limit "
				+ request.parameterMarker( request.jdbcParameterCount() + 1 )
				+ ","
				+ Integer.MAX_VALUE;
	}

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s+lock\\s+in\\s+shared\\s+mode\\b|\\s*;?\\s*$", CASE_INSENSITIVE);

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

}
