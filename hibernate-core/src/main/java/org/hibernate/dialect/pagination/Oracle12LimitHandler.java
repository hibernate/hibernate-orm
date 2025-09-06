/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.pagination;

import java.util.Locale;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.query.spi.Limit;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;

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
		return processSql( sql, -1, limit, null, queryOptions );
	}

	@Override
	public String processSql(String sql, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		return processSql( sql, jdbcParameterCount, queryOptions.getLimit(), parameterMarkerStrategy, queryOptions );
	}

	private String processSql(String sql, int jdbcParameterCount, @Nullable Limit limit, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, QueryOptions queryOptions) {
		final boolean hasFirstRow = hasFirstRow( limit );
		final boolean hasMaxRows = hasMaxRows( limit );

		if ( !hasFirstRow && !hasMaxRows ) {
			return sql;
		}

		return processSql(
				sql,
				hasFirstRow,
				hasMaxRows,
				jdbcParameterCount,
				parameterMarkerStrategy,
				queryOptions.getLockOptions()
		);
	}

	protected String processSql(String sql, boolean hasFirstRow, boolean hasMaxRows, LockOptions lockOptions) {
		return processSql( sql, hasFirstRow, hasMaxRows, -1, null, lockOptions );
	}

	protected String processSql(String sql, boolean hasFirstRow, boolean hasMaxRows, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy, LockOptions lockOptions) {
		if ( lockOptions != null ) {
			final LockMode lockMode = lockOptions.getLockMode();
			return switch ( lockMode ) {
				case PESSIMISTIC_READ, PESSIMISTIC_WRITE, UPGRADE_NOWAIT, PESSIMISTIC_FORCE_INCREMENT, UPGRADE_SKIPLOCKED ->
					processSql( sql, getForUpdateIndex( sql ), hasFirstRow, hasMaxRows, jdbcParameterCount, parameterMarkerStrategy );
				default -> processSqlOffsetFetch( sql, hasFirstRow, hasMaxRows, jdbcParameterCount, parameterMarkerStrategy );
				};
		}
		return processSqlOffsetFetch( sql, hasFirstRow, hasMaxRows, jdbcParameterCount, parameterMarkerStrategy );
	}

	protected String processSqlOffsetFetch(String sql, boolean hasFirstRow, boolean hasMaxRows) {
		return processSqlOffsetFetch( sql, hasFirstRow, hasMaxRows, -1, null );
	}

	protected String processSqlOffsetFetch(String sql, boolean hasFirstRow, boolean hasMaxRows, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy) {

		final int forUpdateLastIndex = getForUpdateIndex( sql );

		if ( forUpdateLastIndex > -1 ) {
			return processSql( sql, forUpdateLastIndex, hasFirstRow, hasMaxRows );
		}

		bindLimitParametersInReverseOrder = false;
		useMaxForLimit = false;
		supportOffset = true;

		final String offsetFetchString;
		if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
			if ( hasFirstRow && hasMaxRows ) {
				offsetFetchString = " offset ? rows fetch next ? rows only";
			}
			else if ( hasFirstRow ) {
				offsetFetchString = " offset ? rows";
			}
			else {
				offsetFetchString = " fetch first ? rows only";
			}
		}
		else {
			final String firstParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
			if ( hasFirstRow && hasMaxRows ) {
				final String secondParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null );
				offsetFetchString = " offset " + firstParameter + " rows fetch next " + secondParameter + " rows only";
			}
			else if ( hasFirstRow ) {
				offsetFetchString = " offset " + firstParameter + " rows";
			}
			else {
				offsetFetchString = " fetch first " + firstParameter + " rows only";
			}
		}

		return insertAtEnd(offsetFetchString, sql);
	}

	protected String processSql(String sql, int forUpdateIndex, boolean hasFirstRow, boolean hasMaxRows) {
		return processSql( sql, forUpdateIndex, hasFirstRow, hasMaxRows, -1, null );
	}

	protected String processSql(String sql, int forUpdateIndex, boolean hasFirstRow, boolean hasMaxRows, int jdbcParameterCount, @Nullable ParameterMarkerStrategy parameterMarkerStrategy) {
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

		if ( ParameterMarkerStrategyStandard.isStandardRenderer( parameterMarkerStrategy ) ) {
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
		}
		else {
			final String firstParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 1, null );
			if ( hasFirstRow && hasMaxRows ) {
				final String secondParameter = parameterMarkerStrategy.createMarker( jdbcParameterCount + 2, null );
				pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 98 );
				pagingSelect.append( "select * from (select row_.*,rownum rownum_ from (" );
				pagingSelect.append( sql );
				pagingSelect.append( ") row_ where rownum<=" );
				pagingSelect.append( firstParameter );
				pagingSelect.append( ") where rownum_>" );
				pagingSelect.append( secondParameter );
			}
			else if ( hasFirstRow ) {
				pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 98 );
				pagingSelect.append( "select * from (" );
				pagingSelect.append( sql );
				pagingSelect.append( ") row_ where rownum>" );
				pagingSelect.append( firstParameter );
			}
			else {
				pagingSelect = new StringBuilder( sql.length() + forUpdateClauseLength + 37 );
				pagingSelect.append( "select * from (" );
				pagingSelect.append( sql );
				pagingSelect.append( ") where rownum<=" );
				pagingSelect.append( firstParameter );
			}
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
