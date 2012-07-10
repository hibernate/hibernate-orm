package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler that delegates all operations to the underlying dialect.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class LegacyLimitHandler extends AbstractLimitHandler {
	private final Dialect dialect;

	public LegacyLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
		this.dialect = dialect;
	}

	public boolean supportsLimit() {
		return dialect.supportsLimit();
	}

	public boolean supportsLimitOffset() {
		return dialect.supportsLimitOffset();
	}

	public boolean supportsVariableLimit() {
		return dialect.supportsVariableLimit();
	}

	public boolean bindLimitParametersInReverseOrder() {
		return dialect.bindLimitParametersInReverseOrder();
	}

	public boolean bindLimitParametersFirst() {
		return dialect.bindLimitParametersFirst();
	}

	public boolean useMaxForLimit() {
		return dialect.useMaxForLimit();
	}

	public boolean forceLimitUsage() {
		return dialect.forceLimitUsage();
	}

	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return dialect.convertToFirstRowValue( zeroBasedFirstResult );
	}

	public String getProcessedSql() {
		boolean useLimitOffset = supportsLimit() && supportsLimitOffset()
				&& LimitHelper.hasFirstRow( selection ) && LimitHelper.hasMaxRows( selection );
		return dialect.getLimitString(
				sql, useLimitOffset ? LimitHelper.getFirstRow( selection ) : 0, getMaxOrLimit()
		);
	}
}
