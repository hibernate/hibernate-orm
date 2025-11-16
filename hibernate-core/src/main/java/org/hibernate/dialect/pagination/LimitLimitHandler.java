/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * Limit handler for MySQL and CUBRID which support the syntax
 * {@code LIMIT n} and {@code LIMIT m, n}. Note that this
 * syntax does not allow specification of an offset without
 * a limit.
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class LimitLimitHandler extends AbstractSimpleLimitHandler {

	public static final LimitLimitHandler INSTANCE = new LimitLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow) {
		return hasFirstRow ? " limit ?,?" : " limit ?";
	}

	@Override
	protected String limitClause(boolean hasFirstRow, int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		final String limit = " limit " + parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
		return hasFirstRow
				? limit + "," + parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null )
				: limit;
	}

	@Override
	protected String offsetOnlyClause() {
		return " limit ?," + Integer.MAX_VALUE;
	}

	@Override
	protected String offsetOnlyClause(int jdbcParameterCount, ParameterMarkerStrategy parameterMarkerStrategy) {
		return " limit "
				+ parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null )
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

	@Override
	public boolean processSqlMutatesState() {
		return false;
	}
}
