/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import java.util.regex.Pattern;

import org.hibernate.dialect.pagination.spi.AbstractSimpleLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

/**
 * A {@link LimitHandler} that works in Interbase and Firebird,
 * using the syntax {@code ROWS n} and {@code ROWS m TO n}.
 * Note that this syntax does not allow specification of an
 * offset without a limit.
 *
 * @author Gavin King
 */
public class RowsLimitHandler extends AbstractSimpleLimitHandler {

	public static final RowsLimitHandler INSTANCE = new RowsLimitHandler();

	@Override
	protected String limitClause(boolean hasFirstRow, PaginationRequest request) {
		final String firstParameter = request.parameterMarker( request.jdbcParameterCount() + 1 );
		if ( hasFirstRow ) {
			return " rows " + firstParameter + " to " + request.parameterMarker( request.jdbcParameterCount() + 2 );
		}
		else {
			return " rows " + firstParameter;
		}
	}

	@Override
	protected String offsetOnlyClause(PaginationRequest request) {
		return " rows " + request.parameterMarker( request.jdbcParameterCount() + 1 ) + " to " + Integer.MAX_VALUE;
	}

	@Override
	public final boolean useMaxForLimit() {
		return true;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult + 1;
	}

	private static final Pattern FOR_UPDATE_PATTERN =
			compile("\\s+for\\s+update\\b|\\s+with\\s+lock\\b|\\s*;?\\s*$", CASE_INSENSITIVE);

	@Override
	protected Pattern getForUpdatePattern() {
		return FOR_UPDATE_PATTERN;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

}
