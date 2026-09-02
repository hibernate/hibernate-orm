/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.pagination.spi.AbstractLimitHandler;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.PaginationRequest;
import org.hibernate.dialect.pagination.spi.PaginationResult;

import java.util.regex.Matcher;

/**
 * A {@link LimitHandler} for Oracle prior to 12c, which uses {@code ROWNUM}.
 */
public class LegacyOracleLimitHandler extends AbstractLimitHandler {
	private final DatabaseVersion version;

	public LegacyOracleLimitHandler(DatabaseVersion version) {
		this.version = version;
	}

	@Override
	public PaginationResult processSql(PaginationRequest request) {
		if ( request.isEmpty() ) {
			return PaginationResult.unchanged( request.sql() );
		}
		final boolean hasOffset = request.hasFirstRow();
		String sql = request.sql().trim();

		String forUpdateClause = null;
		Matcher forUpdateMatcher = getForUpdatePattern().matcher( sql );
		if ( forUpdateMatcher.find() ) {
			int forUpdateIndex = forUpdateMatcher.start();
			// save 'for update ...' and then remove it
			forUpdateClause = sql.substring( forUpdateIndex );
			sql = sql.substring( 0, forUpdateIndex );
		}

		final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
		if ( request.usesStandardParameterMarkers() ) {
			if ( hasOffset ) {
				pagingSelect.append( "select * from (select row_.*,rownum rownum_ from (" ).append( sql );
				if ( version.isBefore( 9 ) ) {
					pagingSelect.append( ") row_) where rownum_<=? and rownum_>?" );
				}
				else {
					pagingSelect.append( ") row_ where rownum<=?) where rownum_>?" );
				}
			}
			else {
				pagingSelect.append( "select * from (" ).append( sql ).append( ") where rownum<=?" );
			}
		}
		else {
			final String firstParameter = request.parameterMarker( request.jdbcParameterCount() + 1 );
			if ( hasOffset ) {
				final String secondParameter = request.parameterMarker( request.jdbcParameterCount() + 2 );
				pagingSelect.append( "select * from (select row_.*,rownum rownum_ from (" ).append( sql );
				if ( version.isBefore( 9 ) ) {
					pagingSelect.append( ") row_) where rownum_<=" );
					pagingSelect.append( firstParameter );
					pagingSelect.append( " and rownum_>" );
					pagingSelect.append( secondParameter );
				}
				else {
					pagingSelect.append( ") row_ where rownum<=" );
					pagingSelect.append( firstParameter );
					pagingSelect.append( ") where rownum_>" );
					pagingSelect.append( secondParameter );
				}
			}
			else {
				pagingSelect.append( "select * from (" ).append( sql ).append( ") where rownum<=" );
				pagingSelect.append( firstParameter );
			}
		}

		if ( forUpdateClause != null ) {
			pagingSelect.append( forUpdateClause );
		}

		return result( request, pagingSelect.toString() );
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return true;
	}

	@Override
	public boolean forceLimitUsage() {
		return true;
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

}
