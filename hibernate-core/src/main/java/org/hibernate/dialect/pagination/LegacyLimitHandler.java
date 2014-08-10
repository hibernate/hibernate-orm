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

import org.hibernate.engine.spi.RowSelection;

/**
 * Limit handler that delegates all operations to the underlying dialect.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class LegacyLimitHandler extends AbstractLimitHandler {

	public static final LegacyLimitHandler INSTANCE = new LegacyLimitHandler();

	/**
	 * Constructs a LegacyLimitHandler
	 */
	private LegacyLimitHandler() {
        // NOP
	}

	@Override
	public boolean supportsLimit() {
		return false;
	}

	@Override
	public boolean supportsLimitOffset() {
		return supportsLimit();
	}

	@Override
	public boolean supportsVariableLimit() {
		return supportsLimit();
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false;
	}

	@Override
	public boolean useMaxForLimit() {
		return false;
	}

	@Override
	public boolean forceLimitUsage() {
		return false;
	}

	@Override
	public int convertToFirstRowValue(int zeroBasedFirstResult) {
		return zeroBasedFirstResult;
	}

	@Override
	public String processSql(String sql, RowSelection selection) {
		throw new UnsupportedOperationException( "Paged queries not supported by " + getClass().getName());
	}
}
