package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler that delegates all operations to the underlying dialect.
 *
 * @author Esen Sagynov (kadishmal at gmail dot com)
 */
public class CUBRIDLimitHandler extends AbstractLimitHandler {
	private final Dialect dialect;

	public CUBRIDLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
		this.dialect = dialect;
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public String getProcessedSql() {
		boolean useLimitOffset = supportsLimit() && supportsLimitOffset()
				&& LimitHelper.hasFirstRow( selection ) && LimitHelper.hasMaxRows( selection );
		return dialect.getLimitString(
				sql, useLimitOffset ? LimitHelper.getFirstRow( selection ) : 0, getMaxOrLimit()
		);
	}
}
