/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import java.util.Locale;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;

/**
 * A {@link LimitHandler} for databases which support the
 * ANSI SQL standard syntax {@code FETCH FIRST m ROWS ONLY}
 * and {@code OFFSET n ROWS FETCH NEXT m ROWS ONLY}.
 *
 * @author Gavin King
 */
public class Oracle12LimitHandler extends AbstractLimitHandler {

	private boolean bindLimitParametersInReverseOrder;
	private boolean useMaxForLimit;
	private boolean supportOffset;

	public static final Oracle12LimitHandler INSTANCE = new Oracle12LimitHandler();

	Oracle12LimitHandler() {
	}

	@Override
	public String processSql(String sql, Limit limit, QueryOptions queryOptions) {
		final boolean hasFirstRow = hasFirstRow( limit );
		final boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		return processSql(
				sql,
				hasFirstRow,
				hasMaxRows,
				queryOptions.getLockOptions()
		);
	}

	protected String processSql(String sql, boolean hasFirstRow, boolean hasMaxRows, LockOptions lockOptions) {
		if ( lockOptions != null ) {
			final LockMode lockMode = lockOptions.getLockMode();
			switch ( lockMode ) {
				case PESSIMISTIC_READ:
				case PESSIMISTIC_WRITE:
				case UPGRADE_NOWAIT:
				case PESSIMISTIC_FORCE_INCREMENT:
				case UPGRADE_SKIPLOCKED: {
					return processSql( sql, getForUpdateIndex( sql ), hasFirstRow, hasMaxRows );
				}
				default: {
					return processSqlOffsetFetch( sql, hasFirstRow, hasMaxRows );
				}
			}
		}
		return processSqlOffsetFetch( sql, hasFirstRow, hasMaxRows );
	}

	protected String processSqlOffsetFetch(String sql, boolean hasFirstRow, boolean hasMaxRows) {

		final int forUpdateLastIndex = getForUpdateIndex( sql );

		if ( forUpdateLastIndex > -1 ) {
			return processSql( sql, forUpdateLastIndex, hasFirstRow, hasMaxRows );
		}

		bindLimitParametersInReverseOrder = false;
		useMaxForLimit = false;
		supportOffset = true;

		final String offsetFetchString;
		if ( hasFirstRow && hasMaxRows ) {
			offsetFetchString = " offset ? rows fetch next ? rows only";
		}
		else if ( hasFirstRow ) {
			offsetFetchString = " offset ? rows";
		}
		else {
			offsetFetchString = " fetch first ? rows only";
		}

		return insertAtEnd(offsetFetchString, sql);
	}

	protected String processSql(String sql, int forUpdateIndex, boolean hasFirstRow, boolean hasMaxRows) {
		bindLimitParametersInReverseOrder = true;
		useMaxForLimit = true;
		supportOffset = false;

		String forUpdateClause = null;
		boolean isForUpdate = false;
		if ( forUpdateIndex > -1 ) {
			// save 'for update ...' and then remove it
			forUpdateClause = sql.substring( forUpdateIndex );
			sql = sql.substring( 0, forUpdateIndex - 1 );
			isForUpdate = true;
		}

		final StringBuilder pagingSelect;

		final int forUpdateClauseLength;
		if ( forUpdateClause == null ) {
			forUpdateClauseLength = 0;
		}
		else {
			forUpdateClauseLength = forUpdateClause.length() + 1;
		}

		if ( hasFirstRow && hasMaxRows ) {
			pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 98 );
			pagingSelect.append( "select * from (select row_.*,rownum rownum_ from (" );
			pagingSelect.append( sql );
			pagingSelect.append( ") row_ where rownum<=?) where rownum_>?" );
		}
		else if ( hasFirstRow ) {
			pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 98 );
			pagingSelect.append( "select * from (" );
			pagingSelect.append( sql );
			pagingSelect.append( ") row_ where rownum>?" );
		}
		else {
			pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 37 );
			pagingSelect.append( "select * from (" );
			pagingSelect.append( sql );
			pagingSelect.append( ") where rownum<=?" );
		}

		if ( isForUpdate ) {
			pagingSelect.append( " " );
			pagingSelect.append( forUpdateClause );
		}

		return pagingSelect.toString();
	}

	private int getForUpdateIndex(String sql) {
		final int forUpdateLastIndex = sql.toLowerCase( Locale.ROOT ).lastIndexOf( "for update" );
		// We need to recognize cases like : select a from t where b = 'for update';
		final int lastIndexOfQuote = sql.lastIndexOf( '\'' );
		if ( forUpdateLastIndex > -1 ) {
			if ( lastIndexOfQuote == -1 ) {
				return forUpdateLastIndex;
			}
			if ( lastIndexOfQuote > forUpdateLastIndex ) {
				return -1;
			}
			return forUpdateLastIndex;
		}
		return forUpdateLastIndex;
	}

	@Override
	public final boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsOffset() {
		return supportOffset;
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return bindLimitParametersInReverseOrder;
	}

	@Override
	public boolean useMaxForLimit() {
		return useMaxForLimit;
	}
}
