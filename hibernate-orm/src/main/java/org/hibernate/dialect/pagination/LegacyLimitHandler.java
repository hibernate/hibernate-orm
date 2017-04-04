/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.pagination;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler that delegates all operations to the underlying dialect.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@SuppressWarnings("deprecation")
public class LegacyLimitHandler extends AbstractLimitHandler {
	private final Dialect dialect;

	/**
	 * Constructs a LegacyLimitHandler
	 *
	 * @param dialect The dialect
	 */
	public LegacyLimitHandler(Dialect dialect) {
		this.dialect = dialect;
	}

	@Override
	public boolean supportsLimit() {
		return dialect.supportsLimit();
	}

	@Override
	public boolean supportsLimitOffset() {
		return dialect.supportsLimitOffset();
	}

	@Override
	public boolean supportsVariableLimit() {
		return dialect.supportsVariableLimit();
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return dialect.bindLimitParametersInReverseOrder();
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return dialect.bindLimitParametersFirst();
	}

	@Override
	public boolean useMaxForLimit() {
		return dialect.useMaxForLimit();
	}

	@Override
	public boolean forceLimitUsage() {
		return dialect.forceLimitUsage();
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return dialect.convertToFirstRowValue( zeroBasedFirstResult );
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		final boolean useLimitOffset = supportsLimit()
				&& supportsLimitOffset()
				&& LimitHelper.hasFirstRow( selection )
				&& LimitHelper.hasMaxRows( selection );
		return dialect.getLimitString(
				sql,
				useLimitOffset ? LimitHelper.getFirstRow( selection ) : 0,
				getMaxOrLimit( selection )
		);
	}
}
