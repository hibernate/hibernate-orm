/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Superclass for {@link LimitHandler}s that don't support
 * offsets at all.
 *
 * @author Gavin King
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractNoOffsetLimitHandler extends AbstractLimitHandler {

	private final boolean variableLimit;

	@SPI({ USE, IMPLEMENT })
	public AbstractNoOffsetLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@SPI(SPI.Role.IMPLEMENT)
	protected abstract String limitClause(PaginationRequest request);

	@SPI(SPI.Role.IMPLEMENT)
	protected abstract String insert(String limitClause, String sql);

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		if ( request.hasMaxRows() ) {
			final String limitClause = limitClause(
					supportsVariableLimit() ? request : withStandardMarker( request )
			);
			final String renderedClause = supportsVariableLimit()
					? limitClause
					: limitClause.replace( "?", Integer.toString( getMaxOrLimit( request ) ) );
			return result( request, insert( renderedClause, request.sql() ) );
		}
		else {
			return result( request, request.sql() );
		}
	}

	private static PaginationRequest withStandardMarker(PaginationRequest request) {
		return new PaginationRequest(
				request.sql(),
				request.firstRow(),
				request.maxRows(),
				request.jdbcParameterCount(),
				null
		);
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@Override
	@SPI(SPI.Role.IMPLEMENT)
	public abstract boolean bindLimitParametersFirst();

}
