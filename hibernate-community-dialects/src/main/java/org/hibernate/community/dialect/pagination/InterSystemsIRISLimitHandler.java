/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;

import java.util.Locale;

public class InterSystemsIRISLimitHandler extends AbstractLimitHandler {
	public static final InterSystemsIRISLimitHandler INSTANCE = new InterSystemsIRISLimitHandler(true);

	private final boolean variableLimit;

	public InterSystemsIRISLimitHandler(boolean variableLimit) {
		this.variableLimit = variableLimit;
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		final String sql = request.sql();
		boolean hasFirstRow = request.hasFirstRow();
		boolean hasMaxRows = request.hasMaxRows();

		if ( !hasFirstRow && !hasMaxRows ) {
			return PaginationResult.unchanged( sql );
		}

		String lowersql = sql.toLowerCase( Locale.ROOT );
		int selectIndex = lowersql.indexOf( "select" );
		if ( hasFirstRow && hasMaxRows ) {
			return result( request, new StringBuilder( sql.length() + 27 )
					.append( sql )
					.insert( selectIndex + 6, " %ROWOFFSET ? %ROWLIMIT ? " )
					.toString() );

		}
		else if ( hasFirstRow ) {
			return result( request, new StringBuilder( sql.length() + 15 )
					.append( sql )
					.insert( selectIndex + 6, " %ROWOFFSET ? " )
					.toString() );
		}
		else {
			final int selectDistinctIndex = lowersql.indexOf( "select distinct" );
			final int insertionPoint = selectIndex + ( selectDistinctIndex == selectIndex ? 15 : 6 );

			return result( request, new StringBuilder( sql.length() + 8 )
					.append( sql )
					.insert( insertionPoint, " TOP ? " )
					.toString() );
		}
	}


	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public final boolean supportsOffset() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public final boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return false;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return true;
	}

}
