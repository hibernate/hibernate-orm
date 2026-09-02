/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;

/**
 * A {@link LimitHandler} for Firebird 2.5 and older which supports the syntax
 * {@code FIRST n SKIP m}.
 */
public class FirstSkipLimitHandler extends AbstractLimitHandler {

	public static final FirstSkipLimitHandler INSTANCE = new FirstSkipLimitHandler();

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		boolean hasFirstRow = request.hasFirstRow();
		boolean hasMaxRows = request.hasMaxRows();

		if ( !hasFirstRow && !hasMaxRows ) {
			return PaginationResult.unchanged( request.sql() );
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( request.usesStandardParameterMarkers() ) {
			if ( hasMaxRows ) {
				skipFirst.append( " first ?" );
			}
			if ( hasFirstRow ) {
				skipFirst.append( " skip ?" );
			}
		}
		else {
			String marker = request.parameterMarker( request.jdbcParameterCount() + 1 );
			if ( hasMaxRows ) {
				skipFirst.append( " first " );
				skipFirst.append( marker );
				marker = request.parameterMarker( request.jdbcParameterCount() + 2 );
			}
			if ( hasFirstRow ) {
				skipFirst.append( " skip " );
				skipFirst.append( marker );
			}
		}
		return result( request, insertAfterSelect( skipFirst.toString(), request.sql() ) );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return request.hasMaxRows()
				? request.hasFirstRow() ? 3 : 2
				: request.hasFirstRow() ? 2 : 1;
	}

}
