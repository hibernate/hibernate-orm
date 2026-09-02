/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;



/**
 * Handler not supporting query LIMIT clause. JDBC API is used to set maximum number of returned rows.
 *
 * @author Lukasz Antoniak
 * @since 8.0
 */
@SPI
public final class NoopLimitHandler extends AbstractLimitHandler {

	public static final NoopLimitHandler INSTANCE = new NoopLimitHandler();

	private NoopLimitHandler() {
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		final Integer jdbcMaxRows;
		if ( request.hasMaxRows() ) {
			final int convertedMaxRows =
					request.maxRows()
					+ convertToFirstRowValue( request.firstRowOrZero() );
			// Use Integer.MAX_VALUE on overflow
			jdbcMaxRows = convertedMaxRows < 0 ? Integer.MAX_VALUE : convertedMaxRows;
		}
		else {
			jdbcMaxRows = null;
		}
		return new PaginationResult(
				request.sql(),
				new PaginationJdbcInstructions(
						java.util.List.of(),
						java.util.List.of(),
						jdbcMaxRows,
						request.firstRowOrZero()
				)
		);
	}
}
