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
 * A {@link LimitHandler} for Informix which supports the syntax
 * {@code SKIP m FIRST n}.
 */
public class SkipFirstLimitHandler extends AbstractLimitHandler {

	public static final SkipFirstLimitHandler INSTANCE = new SkipFirstLimitHandler(true);

	private final boolean variableLimit;

	public SkipFirstLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		boolean hasFirstRow = request.hasFirstRow();
		boolean hasMaxRows = request.hasMaxRows();

		if ( !hasFirstRow && !hasMaxRows ) {
			return PaginationResult.unchanged( request.sql() );
		}

		StringBuilder skipFirst = new StringBuilder();

		if ( supportsVariableLimit() ) {
			if ( request.usesStandardParameterMarkers() ) {
				if ( hasFirstRow ) {
					skipFirst.append( " skip ?" );
				}
				if ( hasMaxRows ) {
					skipFirst.append( " first ?" );
				}
			}
			else {
				String marker = request.parameterMarker( 1 );
				if ( hasMaxRows ) {
					skipFirst.append( " skip " );
					skipFirst.append( marker );
					marker = request.parameterMarker( 2 );
				}
				if ( hasFirstRow ) {
					skipFirst.append( " first " );
					skipFirst.append( marker );
				}
			}
		}
		else {
			if ( hasFirstRow ) {
				skipFirst.append( " skip " )
						.append( request.firstRow() );
			}
			if ( hasMaxRows ) {
				skipFirst.append( " first " )
						.append( getMaxOrLimit( request ) );
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
	public final boolean bindLimitParametersFirst() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	public int parameterPositionStart(PaginationRequest request) {
		return supportsVariableLimit() && request.hasMaxRows()
				? request.hasFirstRow() ? 3 : 2
				: supportsVariableLimit() && request.hasFirstRow() ? 2 : 1;
	}
}
