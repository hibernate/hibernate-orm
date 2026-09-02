/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Superclass for simple {@link LimitHandler}s that don't
 * support specifying an offset without a limit.
 *
 * @author Gavin King
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public abstract class AbstractSimpleLimitHandler extends AbstractLimitHandler {
	@SPI(IMPLEMENT)
	protected AbstractSimpleLimitHandler() {
	}

	@SPI(SPI.Role.IMPLEMENT)
	protected abstract String limitClause(boolean hasFirstRow, PaginationRequest request);

	@SPI(SPI.Role.IMPLEMENT)
	protected String offsetOnlyClause(PaginationRequest request) {
		return null;
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		final boolean hasMaxRows = request.hasMaxRows();
		final boolean hasFirstRow = request.hasFirstRow();
		final String sql = request.sql();
		if ( hasMaxRows ) {
			final String limitClause = limitClause( hasFirstRow, request );
			return result( request, insert( limitClause, sql ) );
		}
		else if ( hasFirstRow ) {
			final String offsetOnlyClause = offsetOnlyClause( request );
			return result( request, offsetOnlyClause != null
					? insert( offsetOnlyClause, sql )
					: sql );
		}
		else {
			return PaginationResult.unchanged( sql );
		}
	}

	@SPI(SPI.Role.IMPLEMENT)
	protected String insert(String limitClause, String sql) {
		return insertBeforeForUpdate( limitClause, sql );
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return super.supportsOffset();
	}
}
