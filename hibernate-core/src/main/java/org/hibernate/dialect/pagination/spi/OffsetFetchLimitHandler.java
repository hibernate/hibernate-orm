/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * A {@link LimitHandler} for databases which support the
 * ANSI SQL standard syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 * @since 8.0
 */
@SPI({ USE, IMPLEMENT })
public class OffsetFetchLimitHandler extends AbstractLimitHandler {

	public static final OffsetFetchLimitHandler INSTANCE = new OffsetFetchLimitHandler(true);

	private final boolean variableLimit;

	@SPI({ USE, IMPLEMENT })
	public OffsetFetchLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		final boolean hasFirstRow = request.hasFirstRow();
		final boolean hasMaxRows = request.hasMaxRows();
		final String sql = request.sql();

		if ( !hasFirstRow && !hasMaxRows ) {
			return PaginationResult.unchanged( sql );
		}

		StringBuilder offsetFetch = new StringBuilder();

		begin(sql, offsetFetch, hasFirstRow, hasMaxRows);

		if ( hasFirstRow ) {
			offsetFetch.append( " offset " );
			if ( supportsVariableLimit() ) {
				if ( request.usesStandardParameterMarkers() ) {
					offsetFetch.append( "?" );
				}
				else {
					offsetFetch.append( request.parameterMarker( request.jdbcParameterCount() + 1 ) );
				}
			}
			else {
					offsetFetch.append( request.firstRow() );
			}
			if ( renderOffsetRowsKeyword() ) {
				offsetFetch.append( " rows" );
			}

		}
		if ( hasMaxRows ) {
			if ( hasFirstRow ) {
				offsetFetch.append( " fetch next " );
			}
			else {
				offsetFetch.append( " fetch first " );
			}
			if ( supportsVariableLimit() ) {
				if ( request.usesStandardParameterMarkers() ) {
					offsetFetch.append( "?" );
				}
				else {
					offsetFetch.append(
							request.parameterMarker( request.jdbcParameterCount() + (hasFirstRow ? 2 : 1) ) );
				}
			}
			else {
				offsetFetch.append( getMaxOrLimit( request ) );
			}
			offsetFetch.append( " rows only" );
		}

		return result( request, insert( offsetFetch.toString(), sql ) );
	}

	@SPI(SPI.Role.IMPLEMENT)
	protected void begin(String sql, StringBuilder offsetFetch, boolean hasFirstRow, boolean hasMaxRows) {}

	@SPI(SPI.Role.IMPLEMENT)
	protected String insert(String offsetFetch, String sql) {
		return insertBeforeForUpdate( offsetFetch, sql );
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
	public final boolean supportsVariableLimit() {
		return variableLimit;
	}

	@SPI(SPI.Role.IMPLEMENT)
	protected boolean renderOffsetRowsKeyword() {
		return true;
	}

}
