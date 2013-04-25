/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
	 * @param sql The sql
	 * @param selection The row selection
	 */
	public LegacyLimitHandler(Dialect dialect, String sql, RowSelection selection) {
		super( sql, selection );
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
	public String getProcessedSql() {
		final boolean useLimitOffset = supportsLimit()
				&& supportsLimitOffset()
				&& LimitHelper.hasFirstRow( selection )
				&& LimitHelper.hasMaxRows( selection );
		return dialect.getLimitString(
				sql,
				useLimitOffset ? LimitHelper.getFirstRow( selection ) : 0,
				getMaxOrLimit()
		);
	}
}
