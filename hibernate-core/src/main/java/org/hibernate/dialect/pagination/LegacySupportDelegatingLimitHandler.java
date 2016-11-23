/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * @author Chris Cranford
 */
public class LegacySupportDelegatingLimitHandler implements LimitHandler {

	private final Dialect dialect;
	private final LimitHandler legacyHandler;
	private final LimitHandler improvedHandler;
	private LimitHandler delegate;

	public LegacySupportDelegatingLimitHandler(Dialect dialect, LimitHandler legacyHandler, LimitHandler improvedHandler) {
		this.dialect = dialect;
		this.legacyHandler = legacyHandler;
		this.improvedHandler = improvedHandler;
	}

	@Override
	public boolean supportsLimit() {
		return getLimitHandler().supportsLimit();
	}

	@Override
	public boolean supportsLimitOffset() {
		return getLimitHandler().supportsLimitOffset();
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		return getLimitHandler().processSql( sql, selection );
	}

	@Override
	public int bindLimitParametersAtStartOfQuery(RowSelection selection, PreparedStatement statement, int index) throws SQLException {
		return getLimitHandler().bindLimitParametersAtStartOfQuery( selection, statement, index );
	}

	@Override
	public int bindLimitParametersAtEndOfQuery(RowSelection selection, PreparedStatement statement, int index) throws SQLException {
		return getLimitHandler().bindLimitParametersAtEndOfQuery( selection, statement, index );
	}

	@Override
	public void setMaxRows(RowSelection selection, PreparedStatement statement) throws SQLException {
		getLimitHandler().setMaxRows( selection, statement );
	}

	private LimitHandler getLimitHandler() {
		if ( delegate == null ) {
			this.delegate = resolveLimitHandler();
		}
		return this.delegate;
	}

	private LimitHandler resolveLimitHandler() {
		if ( dialect.isLegacyLimitHandlerBehaviorEnabled() ) {
			return legacyHandler;
		}
		return improvedHandler;
	}
}
