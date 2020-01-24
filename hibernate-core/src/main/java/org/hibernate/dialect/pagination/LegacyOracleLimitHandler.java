/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.engine.spi.RowSelection;

import java.util.regex.Matcher;

/**
 * A {@link LimitHandler} for Oracle prior to 12c, which uses {@code ROWNUM}.
 */
public class LegacyOracleLimitHandler extends AbstractLimitHandler {

	private int version;

	public LegacyOracleLimitHandler(int version) {
		this.version = version;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		final boolean hasOffset = hasFirstRow( selection );
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
			pagingSelect.append( "select * from (select row_.*, rownum rownum_ from (" ).append( sql );
			if ( version < 9 ) {
				pagingSelect.append( ") row_) where rownum_ <= ? and rownum_ > ?" );
			}
			else {
				pagingSelect.append( ") row_ where rownum <= ?) where rownum_ > ?" );
			}
		}
		else {
			pagingSelect.append( "select * from (" ).append( sql ).append( ") where rownum <= ?" );
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
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}
}
