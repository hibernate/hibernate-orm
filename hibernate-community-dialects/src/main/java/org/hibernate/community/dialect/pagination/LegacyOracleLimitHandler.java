/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.pagination;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.query.spi.Limit;

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
	public String processSql(String sql, Limit limit) {
		final boolean hasOffset = hasFirstRow( limit );
		sql = sql.trim();

		String forUpdateClause = null;
		Matcher forUpdateMatcher = getForUpdatePattern().matcher( sql );
		if ( forUpdateMatcher.find() ) {
			int forUpdateIndex = forUpdateMatcher.start();
			// save 'for update ...' and then remove it
			forUpdateClause = sql.substring( forUpdateIndex );
			sql = sql.substring( 0, forUpdateIndex );
		}

		final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
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

		if ( forUpdateClause != null ) {
			pagingSelect.append( forUpdateClause );
		}

		return pagingSelect.toString();
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
